package com.knknkn92.craftymobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knknkn92.craftymobile.data.api.CraftyApiFactory
import com.knknkn92.craftymobile.data.api.ServerInfo
import com.knknkn92.craftymobile.data.api.ServerStats
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServerWithStats(
    val info: ServerInfo,
    val stats: ServerStats? = null,
)

/** ServerTab + VPSステータス集計用のstate */
data class DashboardUiState(
    val servers: List<ServerWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,   // 実行中アクションのserverId
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
) {
    // VPSステータスをサーバー一覧から集計
    val totalPlayers: Int get() = servers.sumOf { it.stats?.online ?: 0 }
    val totalMaxPlayers: Int get() = servers.sumOf { it.stats?.max ?: 0 }
    // CPU/MEMは全サーバーの平均
    val avgCpu: Float get() = if (servers.isEmpty()) 0f
        else servers.map { it.stats?.cpu ?: 0f }.average().toFloat()
    val avgMem: Float get() = if (servers.isEmpty()) 0f
        else servers.map { it.stats?.memPercent ?: 0f }.average().toFloat()
}

class DashboardViewModel(
    val baseUrl: String,
    val token: String,
) : ViewModel() {

    private val bearerToken get() = "Bearer $token"
    private val api by lazy { CraftyApiFactory.create(baseUrl) }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadServers() }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val serversResponse = api.getServers(bearerToken)
                if (!serversResponse.isSuccessful || serversResponse.body()?.status != "ok") {
                    _uiState.update {
                        it.copy(isLoading = false,
                            errorMessage = "Failed to load servers (${serversResponse.code()})")
                    }
                    return@launch
                }
                val servers = serversResponse.body()?.data ?: emptyList()

                // statsを並列取得
                val serversWithStats = servers.map { server ->
                    async {
                        val statsResp = runCatching { api.getServerStats(bearerToken, server.serverId) }.getOrNull()
                        ServerWithStats(
                            info  = server,
                            stats = if (statsResp?.isSuccessful == true) statsResp.body()?.data else null,
                        )
                    }
                }.awaitAll()

                _uiState.update { it.copy(isLoading = false, servers = serversWithStats) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Connection error: ${e.message}") }
            }
        }
    }

    /** Stop / Restart / Kill / Start */
    fun serverAction(serverId: String, action: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = serverId) }
            try {
                val resp = api.serverAction(bearerToken, serverId, action)
                val msg = if (resp.isSuccessful) "$action sent successfully."
                          else "Failed: ${resp.code()}"
                _uiState.update { it.copy(actionInProgress = null, snackbarMessage = msg) }
                // 少し待ってからstatsを再取得
                kotlinx.coroutines.delay(2000)
                loadServers()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionInProgress = null, snackbarMessage = "Error: ${e.message}")
                }
            }
        }
    }

    fun dismissError()   = _uiState.update { it.copy(errorMessage = null) }
    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    class Factory(private val baseUrl: String, private val token: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(baseUrl, token) as T
    }
}
