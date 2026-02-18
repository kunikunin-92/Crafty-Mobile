package com.knknkn92.craftymobile.data.api

import com.google.gson.annotations.SerializedName

// ---- リクエスト ----

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("totp")    val totp: String? = null,  // MFA有効時のみ (6桁数字)
)

// ---- レスポンス共通 ----

data class LoginResponse(
    @SerializedName("status") val status: String,        // "ok" or "error"
    @SerializedName("data")   val data: LoginData? = null,
    @SerializedName("error")  val error: String? = null,
    @SerializedName("error_data") val errorData: String? = null,
)

data class LoginData(
    @SerializedName("token")   val token: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("warning") val warning: String? = null, // バックアップコード使用時
)
