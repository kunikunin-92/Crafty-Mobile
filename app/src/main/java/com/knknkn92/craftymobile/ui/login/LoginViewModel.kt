package com.knknkn92.craftymobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.knknkn92.craftymobile.data.api.CraftyApiFactory
import com.knknkn92.craftymobile.data.api.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverAddress: String = "",
    val username: String = "",
    val password: String = "",
    val mfaCode: String = "",
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val token: String? = null,
    val userId: String? = null,
    val errorMessage: String? = null,
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onServerAddressChange(value: String) =
        _uiState.update { it.copy(serverAddress = value, errorMessage = null) }

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onMfaCodeChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 6) {
            _uiState.update { it.copy(mfaCode = value, errorMessage = null) }
        }
    }

    fun login() {
        val state = _uiState.value

        // 入力バリデーション
        if (state.serverAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "サーバーアドレスを入力してください") }
            return
        }
        if (state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ユーザー名を入力してください") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "パスワードを入力してください") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val api = CraftyApiFactory.create(state.serverAddress)
                val request = LoginRequest(
                    username = state.username,
                    password = state.password,
                    totp = state.mfaCode.ifBlank { null },
                )
                val response = api.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "ok" && body.data != null) {
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                loginSuccess = true,
                                token        = body.data.token,
                                userId       = body.data.userId,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                errorMessage = body?.errorData ?: "ログインに失敗しました",
                            )
                        }
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401  -> "ユーザー名またはパスワードが正しくありません"
                        429  -> "ログイン試行回数が多すぎます。しばらく待ってから再試行してください"
                        403  -> "アカウントが無効化されています"
                        else -> "エラーが発生しました (${response.code()})"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "サーバーに接続できません。アドレスを確認してください"
                    e.message?.contains("timeout") == true ->
                        "接続がタイムアウトしました"
                    else -> "接続エラー: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }

    fun dismissSuccess() = _uiState.update { it.copy(loginSuccess = false) }
    fun dismissError()   = _uiState.update { it.copy(errorMessage = null) }
}
