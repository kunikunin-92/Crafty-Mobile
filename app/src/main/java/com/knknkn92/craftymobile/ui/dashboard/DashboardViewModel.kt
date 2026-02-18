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

data class DashboardUiState(
    val servers: List<ServerWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class DashboardViewModel(
    private val baseUrl: String,
    private val token: String,
) : ViewModel() {

    private val bearerToken get() = "Bearer $token"

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadServers()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val api = CraftyApiFactory.create(baseUrl)

                // サーバー一覧取得
                val serversResponse = api.getServers(bearerToken)
                if (!serversResponse.isSuccessful || serversResponse.body()?.status != "ok") {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            errorMessage = "Failed to load servers (${serversResponse.code()})",
                        )
                    }
                    return@launch
                }

                val servers = serversResponse.body()?.data ?: emptyList()

                // 各サーバーのstatsを並列取得
                val serversWithStats = servers.map { server ->
                    async {
                        val statsResponse = try {
                            api.getServerStats(bearerToken, server.serverId)
                        } catch (e: Exception) {
                            null
                        }
                        ServerWithStats(
                            info  = server,
                            stats = if (statsResponse?.isSuccessful == true)
                                        statsResponse.body()?.data
                                    else null,
                        )
                    }
                }.awaitAll()

                _uiState.update {
                    it.copy(isLoading = false, servers = serversWithStats)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = "Connection error: ${e.message}",
                    )
                }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    // Factory for passing arguments to ViewModel
    class Factory(private val baseUrl: String, private val token: String) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(baseUrl, token) as T
    }
}
