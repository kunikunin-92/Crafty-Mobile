package com.knknkn92.craftymobile.ui.serverdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knknkn92.craftymobile.data.api.CraftyApiFactory
import com.knknkn92.craftymobile.data.api.ServerInfo
import com.knknkn92.craftymobile.data.api.StdinRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── ログ1行のパース結果 ────────────────────────────
data class ParsedLogLine(
    val raw: String,
    val time: String,
    val level: String,   // INFO / WARN / ERROR / etc.
    val message: String,
)

// ── プレイヤー情報（APIから取れない場合はコマンドで取得）──
data class OnlinePlayer(
    val name: String,
)

// ── UI State ──────────────────────────────────────
data class ServerDetailUiState(
    val serverInfo: ServerInfo,
    // Terminal
    val terminalLines: List<String> = emptyList(),
    val isTerminalLoading: Boolean = false,
    // Log
    val logLines: List<ParsedLogLine> = emptyList(),
    val isLogLoading: Boolean = false,
    val logFilter: String = "all",    // all / info / warn / error
    // Players
    val onlinePlayers: List<OnlinePlayer> = emptyList(),
    val isPlayersLoading: Boolean = false,
    // Action
    val actionInProgress: Boolean = false,
    // Snackbar
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
)

private val LOG_REGEX = Regex("""\[(\d{2}:\d{2}:\d{2})\] \[.*?/(INFO|WARN|ERROR|DEBUG|FATAL)\]: (.*)""")

private fun parseLine(raw: String): ParsedLogLine {
    val match = LOG_REGEX.find(raw)
    return if (match != null) {
        ParsedLogLine(
            raw     = raw,
            time    = match.groupValues[1],
            level   = match.groupValues[2],
            message = match.groupValues[3],
        )
    } else {
        ParsedLogLine(raw = raw, time = "", level = "INFO", message = raw)
    }
}

class ServerDetailViewModel(
    val baseUrl: String,
    val token: String,
    initialServerInfo: ServerInfo,
) : ViewModel() {

    private val bearerToken get() = "Bearer $token"
    private val api by lazy { CraftyApiFactory.create(baseUrl) }
    private val serverId = initialServerInfo.serverId

    private val _uiState = MutableStateFlow(ServerDetailUiState(serverInfo = initialServerInfo))
    val uiState: StateFlow<ServerDetailUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    // ── ログ取得（Terminal / Log 両方に使う）─────────
    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTerminalLoading = true, isLogLoading = true) }
            try {
                val resp = api.getLogs(bearerToken, serverId)
                if (resp.isSuccessful) {
                    val rawLines = resp.body()?.logLines() ?: emptyList()
                    val parsed = rawLines.map { parseLine(it) }
                    _uiState.update {
                        it.copy(
                            terminalLines    = rawLines,
                            logLines         = parsed,
                            isTerminalLoading = false,
                            isLogLoading     = false,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTerminalLoading = false,
                            isLogLoading      = false,
                            snackbarMessage   = "Failed to load logs (${resp.code()})",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTerminalLoading = false,
                        isLogLoading      = false,
                        snackbarMessage   = "Error: ${e.message}",
                    )
                }
            }
        }
    }

    // ── コマンド送信 ─────────────────────────────────
    fun sendCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            try {
                api.sendCommand(bearerToken, serverId, StdinRequest(command))
                // コマンド送信後にログを再取得
                delay(500)
                loadLogs()
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error: ${e.message}") }
            }
        }
    }

    // ── プレイヤー一覧（logsから /list コマンドで取得） ─
    // Crafty Controller の stats には players フィールドがあるが、
    // 詳細API がないため list コマンドを使う実装も考えられる。
    // ここでは stats の players JSON フィールドから名前を取る
    fun loadPlayers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlayersLoading = true) }
            try {
                val statsResp = api.getServerStats(bearerToken, serverId)
                if (statsResp.isSuccessful) {
                    val stats = statsResp.body()?.data
                    // players フィールドは "Player1, Player2" 形式の文字列
                    val names = stats?.players
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: emptyList()
                    _uiState.update {
                        it.copy(
                            onlinePlayers    = names.map { OnlinePlayer(it) },
                            isPlayersLoading = false,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isPlayersLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isPlayersLoading = false, snackbarMessage = "Error: ${e.message}")
                }
            }
        }
    }

    // ── Kick / Ban（コマンド経由）───────────────────
    fun kickPlayer(name: String) {
        sendCommand("kick $name")
        _uiState.update { it.copy(snackbarMessage = "Kicked $name") }
    }

    fun banPlayer(name: String) {
        sendCommand("ban $name")
        _uiState.update { it.copy(snackbarMessage = "Banned $name") }
    }

    fun unbanPlayer(name: String) {
        sendCommand("pardon $name")
        _uiState.update { it.copy(snackbarMessage = "Unbanned $name") }
    }

    // ── Stop / Restart / Kill ────────────────────────
    fun serverAction(action: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                val resp = api.serverAction(bearerToken, serverId, action)
                val msg = if (resp.isSuccessful) "$action sent." else "Failed: ${resp.code()}"
                _uiState.update { it.copy(actionInProgress = false, snackbarMessage = msg) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionInProgress = false, snackbarMessage = "Error: ${e.message}")
                }
            }
        }
    }

    // ── Log filter ───────────────────────────────────
    fun setLogFilter(filter: String) {
        _uiState.update { it.copy(logFilter = filter) }
    }

    // ── Snackbar / Error dismiss ─────────────────────
    fun dismissSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }
    fun dismissError()    = _uiState.update { it.copy(errorMessage = null) }

    // ── Factory ──────────────────────────────────────
    class Factory(
        private val baseUrl: String,
        private val token: String,
        private val serverInfo: ServerInfo,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ServerDetailViewModel(baseUrl, token, serverInfo) as T
    }
}
