package com.knknkn92.craftymobile.data.api

import com.google.gson.annotations.SerializedName

// ============================================================
// Auth
// ============================================================

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("totp")     val totp: String? = null,
)

data class LoginResponse(
    @SerializedName("status")     val status: String,
    @SerializedName("data")       val data: LoginData? = null,
    @SerializedName("error")      val error: String? = null,
    @SerializedName("error_data") val errorData: String? = null,
)

data class LoginData(
    @SerializedName("token")   val token: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("warning") val warning: String? = null,
)

// ============================================================
// Servers
// ============================================================

data class ServersResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data")   val data: List<ServerInfo>? = null,
)

data class ServerInfo(
    @SerializedName("server_id")   val serverId: String,
    @SerializedName("server_name") val serverName: String,
    @SerializedName("type")        val type: String? = null,
    @SerializedName("server_ip")   val serverIp: String? = null,
    @SerializedName("server_port") val serverPort: Int? = null,
)

// ============================================================
// Server Stats
// ============================================================

data class ServerStatsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data")   val data: ServerStats? = null,
)

/**
 * Crafty Controller v4 の /api/v2/servers/{id}/stats レスポンス。
 *
 * 実際の構造:
 * {
 *   "status": "ok",
 *   "data": {
 *     "server_id": { "server_name": "...", "server_ip": "...", "server_port": 25565, "type": "..." },
 *     "running": true,
 *     "crashed": false,
 *     "cpu": 12.3,
 *     "mem": 1024.0,          // MB
 *     "mem_percent": 45.6,
 *     "online": 2,
 *     "max": 20,
 *     "players": "Player1,Player2",
 *     "version": "1.20.4",
 *     ...
 *   }
 * }
 */
data class ServerStats(
    // server_id はネストオブジェクト（サーバー設定情報を含む）
    @SerializedName("server_id")     val serverIdObj: ServerIdInfo? = null,
    @SerializedName("running")       val running: Boolean = false,
    @SerializedName("crashed")       val crashed: Boolean = false,
    @SerializedName("cpu")           val cpu: Float = 0f,
    // mem は "3.7GB" のような文字列で返ってくる
    @SerializedName("mem")           val mem: String? = null,
    @SerializedName("mem_percent")   val memPercent: Float = 0f,
    @SerializedName("online")        val online: Int = 0,
    @SerializedName("max")           val max: Int = 0,
    // players は "[]" や "[\"Player1\"]" のようなJSON文字列
    @SerializedName("players")       val players: String? = null,
    @SerializedName("version")       val version: String? = null,
    @SerializedName("world_name")    val worldName: String? = null,
    @SerializedName("world_size")    val worldSize: String? = null,
    @SerializedName("started")       val started: String? = null,
    @SerializedName("desc")          val desc: String? = null,
    @SerializedName("updating")      val updating: Boolean = false,
    @SerializedName("waiting_start") val waitingStart: Boolean = false,
) {
    /**
     * players フィールドをパースしてプレイヤー名リストを返す。
     * Crafty v4 は players を JSON 配列文字列 "[]" や "[\"Name\"]" で返す。
     */
    fun playerNames(): List<String> {
        val raw = players ?: return emptyList()
        // "[]" または空の場合
        val trimmed = raw.trim()
        if (trimmed == "[]" || trimmed.isBlank()) return emptyList()
        // "[\"Player1\", \"Player2\"]" → リストを手動パース
        return try {
            trimmed
                .removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** stats.server_id の中のサーバー基本情報 */
data class ServerIdInfo(
    @SerializedName("server_id")   val serverId: String? = null,
    @SerializedName("server_name") val serverName: String? = null,
    @SerializedName("server_ip")   val serverIp: String? = null,
    @SerializedName("server_port") val serverPort: Int? = null,
    @SerializedName("type")        val type: String? = null,
)

// ============================================================
// Server Action  POST /api/v2/servers/{id}/action/{action}
// ============================================================

data class ActionResponse(
    @SerializedName("status")     val status: String,
    @SerializedName("error")      val error: String? = null,
    @SerializedName("error_data") val errorData: String? = null,
)

// ============================================================
// Server Logs   GET /api/v2/servers/{id}/logs
// ============================================================
//
// 実際のレスポンス:
// { "status": "ok", "data": ["log line 1", "log line 2", ...] }
// → data が直接 List<String>
//
// Gson で data: List<String> として受け取る。
// TypeToken は CraftyApiService 側でカスタムコンバータを使う必要があるが、
// Retrofit + Gson は List<String> を直接デシリアライズできる。

data class LogsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data")   val data: List<String>? = null,
) {
    fun logLines(): List<String> = data ?: emptyList()
}

// ============================================================
// Stdin (console command)  POST /api/v2/servers/{id}/stdin
// ============================================================

data class StdinRequest(
    @SerializedName("command") val command: String,
)
