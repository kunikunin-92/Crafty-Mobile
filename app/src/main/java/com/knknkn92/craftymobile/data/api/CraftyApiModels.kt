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

data class ServerStats(
    @SerializedName("server_id")       val serverId: String? = null,
    @SerializedName("server_name")     val serverName: String? = null,
    @SerializedName("running")         val running: Boolean = false,
    @SerializedName("crashed")         val crashed: Boolean = false,
    @SerializedName("cpu")             val cpu: Float = 0f,
    @SerializedName("mem")             val mem: Float = 0f,
    @SerializedName("mem_percent")     val memPercent: Float = 0f,
    @SerializedName("online")          val online: Int = 0,
    @SerializedName("max")             val max: Int = 0,
    @SerializedName("players")         val players: String? = null,
    @SerializedName("version")         val version: String? = null,
    @SerializedName("world_name")      val worldName: String? = null,
    @SerializedName("world_size")      val worldSize: String? = null,
    @SerializedName("started")         val started: String? = null,
    @SerializedName("desc")            val desc: String? = null,
    @SerializedName("updating")        val updating: Boolean = false,
    @SerializedName("waiting_start")   val waitingStart: Boolean = false,
)
