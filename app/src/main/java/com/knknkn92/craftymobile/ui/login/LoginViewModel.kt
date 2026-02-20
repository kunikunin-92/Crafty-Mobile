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
    val token: String? = null,
    val userId: String? = null,
    val errorMessage: String? = null,
    // ログイン成功時に baseUrl をセットして画面側で検知する
    val loggedInBaseUrl: String? = null,
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

        if (state.serverAddress.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a server address.") }
            return
        }
        if (state.username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your username.") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // https:// が付いていなければ自動付与
                val rawAddress = state.serverAddress.trim()
                val baseUrl = when {
                    rawAddress.startsWith("https://") -> rawAddress
                    rawAddress.startsWith("http://")  -> rawAddress
                    else -> "https://$rawAddress"
                }

                val api = CraftyApiFactory.create(baseUrl)
                val request = LoginRequest(
                    username = state.username,
                    password = state.password,
                    totp     = state.mfaCode.ifBlank { null },
                )
                val response = api.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "ok" && body.data != null) {
                        // loggedInBaseUrl をセットして LoginScreen 側の LaunchedEffect で検知
                        _uiState.update {
                            it.copy(
                                isLoading      = false,
                                token          = body.data.token,
                                userId         = body.data.userId,
                                serverAddress  = baseUrl,
                                loggedInBaseUrl = baseUrl,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading    = false,
                                errorMessage = body?.errorData ?: "Login failed.",
                            )
                        }
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401  -> "Incorrect username or password."
                        429  -> "Too many login attempts. Please try again later."
                        403  -> "Account is disabled."
                        else -> "Error: ${response.code()}"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Cannot connect to server. Please check the address."
                    e.message?.contains("timeout") == true ->
                        "Connection timed out."
                    else -> "Connection error: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
