package com.knknkn92.craftymobile.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface CraftyApiService {

    // ---- Auth ----
    @POST("api/v2/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ---- Servers ----
    @GET("api/v2/servers")
    suspend fun getServers(
        @Header("Authorization") token: String,
    ): Response<ServersResponse>

    @GET("api/v2/servers/{serverId}/stats")
    suspend fun getServerStats(
        @Header("Authorization") token: String,
        @Path("serverId") serverId: String,
    ): Response<ServerStatsResponse>

    // ---- Actions: start / stop / restart / kill / backup ----
    @POST("api/v2/servers/{serverId}/action/{action}")
    suspend fun serverAction(
        @Header("Authorization") token: String,
        @Path("serverId") serverId: String,
        @Path("action") action: String,
    ): Response<ActionResponse>

    // ---- Console command (stdin) ----
    @POST("api/v2/servers/{serverId}/stdin")
    suspend fun sendCommand(
        @Header("Authorization") token: String,
        @Path("serverId") serverId: String,
        @Body request: StdinRequest,
    ): Response<ActionResponse>

    // ---- Logs ----
    @GET("api/v2/servers/{serverId}/logs")
    suspend fun getLogs(
        @Header("Authorization") token: String,
        @Path("serverId") serverId: String,
    ): Response<LogsResponse>
}

object CraftyApiFactory {
    fun create(baseUrl: String): CraftyApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(loggingInterceptor)
            .build()

        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CraftyApiService::class.java)
    }
}
