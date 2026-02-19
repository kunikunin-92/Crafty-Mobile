package com.knknkn92.craftymobile.ui.serverdetail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.knknkn92.craftymobile.data.api.CraftyApiFactory
import com.knknkn92.craftymobile.data.api.ServerInfo
import com.knknkn92.craftymobile.data.api.StdinRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "ServerDetailVM"
private const val POLL_INTERVAL_MS = 5_000L   // 5秒ポーリング

// ── ログ1行のパース結果 ────────────────────────────
data class ParsedLogLine(
    val raw: String,
    val time: String,
    val level: String,   // INFO / WARN / ERROR / etc.
    val message: String,
)

// ── プレイヤー情報 ─────────────────────────────────
data class OnlinePlayer(
    val name: String,
)

// ── 確認ダイアログ用 ──────────────────────────────
data class PendingAction(
    val action: String,         // "stop_server" / "restart_server" / "kill_server"
    val label: String,          // 表示用ラベル
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
    val pendingAction: PendingAction? = null,   // 確認ダイアログ用
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

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    // ── ポーリング開始 ────────────────────────────────
    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                loadLogsInternal()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // ── ログ取得（内部用・ポーリングから呼ばれる）────
    private suspend fun loadLogsInternal() {
        try {
            val resp = api.getLogs(bearerToken, serverId)
            if (resp.isSuccessful) {
                val rawLines = resp.body()?.logLines() ?: emptyList()
                val parsed = rawLines.map { parseLine(it) }
                _uiState.update {
                    it.copy(
                        terminalLines     = rawLines,
                        logLines          = parsed,
                        isTerminalLoading = false,
                        isLogLoading      = false,
                    )
                }
            } else {
                Log.w(TAG, "getLogs failed: HTTP ${resp.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getLogs error: ${e.message}", e)
        }
    }

    // ── 手動リロード ─────────────────────────────────
    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTerminalLoading = true, isLogLoading = true) }
            loadLogsInternal()
            _uiState.update { it.copy(isTerminalLoading = false, isLogLoading = false) }
        }
    }

    // ── コマンド送信 ─────────────────────────────────
    // Crafty Controller stdin は { "command": "..." } JSON を受け取る
    // コマンドをそのまま送る（HTML エスケープ不要）
    fun sendCommand(command: String) {
        if (command.isBlank()) return
        Log.d(TAG, "sendCommand: '$command'")
        viewModelScope.launch {
            try {
                val resp = api.sendCommand(bearerToken, serverId, StdinRequest(command))
                Log.d(TAG, "sendCommand result: isSuccessful=${resp.isSuccessful} code=${resp.code()}")
                if (!resp.isSuccessful) {
                    _uiState.update { it.copy(snackbarMessage = "Command failed: HTTP ${resp.code()}") }
                }
                // コマンド送信後 1s 待ってからログ更新
                delay(1000)
                loadLogsInternal()
            } catch (e: Exception) {
                Log.e(TAG, "sendCommand error: ${e.message}", e)
                _uiState.update { it.copy(snackbarMessage = "Error: ${e.message}") }
            }
        }
    }

    // ── プレイヤー一覧取得 ───────────────────────────
    // stats.players は "Player1,Player2" または "False" (未接続時) の文字列
    fun loadPlayers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPlayersLoading = true) }
            try {
                val statsResp = api.getServerStats(bearerToken, serverId)
                Log.d(TAG, "getServerStats: isSuccessful=${statsResp.isSuccessful} code=${statsResp.code()}")
                if (statsResp.isSuccessful) {
                    val stats = statsResp.body()?.data
                    Log.d(TAG, "stats: running=${stats?.running} cpu=${stats?.cpu} players='${stats?.players}'")

                    val rawPlayers = stats?.players ?: ""
                    // "False" や空文字列は「プレイヤーなし」として扱う
                    val names = if (rawPlayers.isBlank() || rawPlayers.equals("False", ignoreCase = true)) {
                        emptyList()
                    } else {
                        rawPlayers.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }
                    _uiState.update {
                        it.copy(
                            onlinePlayers    = names.map { OnlinePlayer(it) },
                            isPlayersLoading = false,
                        )
                    }
                } else {
                    Log.w(TAG, "getServerStats failed: HTTP ${statsResp.code()}")
                    _uiState.update {
                        it.copy(
                            isPlayersLoading = false,
                            snackbarMessage  = "Failed to get player list: HTTP ${statsResp.code()}",
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPlayers error: ${e.message}", e)
                _uiState.update {
                    it.copy(isPlayersLoading = false, snackbarMessage = "Error: ${e.message}")
                }
            }
        }
    }

    // ── Kick / Ban（コマンド経由）───────────────────
    fun kickPlayer(name: String) {
        sendCommand("kick $name")
        _uiState.update { it.copy(snackbarMessage = "Kicking $name...") }
    }

    fun banPlayer(name: String) {
        sendCommand("ban $name")
        _uiState.update { it.copy(snackbarMessage = "Banning $name...") }
    }

    fun unbanPlayer(name: String) {
        sendCommand("pardon $name")
        _uiState.update { it.copy(snackbarMessage = "Unbanning $name...") }
    }

    // ── Stop / Restart / Kill（確認ダイアログ経由）──
    fun requestAction(action: String) {
        val label = when (action) {
            "stop_server"    -> "Stop"
            "restart_server" -> "Restart"
            "kill_server"    -> "Kill"
            "start_server"   -> "Start"
            else             -> action
        }
        _uiState.update { it.copy(pendingAction = PendingAction(action, label)) }
    }

    fun confirmAction() {
        val pending = _uiState.value.pendingAction ?: return
        _uiState.update { it.copy(pendingAction = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                val resp = api.serverAction(bearerToken, serverId, pending.action)
                val msg = if (resp.isSuccessful) "${pending.label} command sent."
                          else "Failed: HTTP ${resp.code()}"
                _uiState.update { it.copy(actionInProgress = false, snackbarMessage = msg) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(actionInProgress = false, snackbarMessage = "Error: ${e.message}")
                }
            }
        }
    }

    fun cancelAction() {
        _uiState.update { it.copy(pendingAction = null) }
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
