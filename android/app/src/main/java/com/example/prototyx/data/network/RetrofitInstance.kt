package com.example.prototyx.data.network

import com.example.prototyx.data.security.AuthManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    private var baseUrl = "https://prototyx.onrender.com/"
    private var authManager: AuthManager? = null

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        authManager?.getToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        val response = chain.proceed(requestBuilder.build())
        if (!response.isSuccessful) {
            if (response.code == 401) authManager?.clearToken()
            val detail = try {
                com.google.gson.JsonParser.parseString(response.peekBody(8192).string()).asJsonObject.get("detail")?.let {
                    if (it.isJsonPrimitive) it.asString else "Check the entered values."
                }
            } catch (_: Exception) { null }
            val message = if (response.code == 401) "Session expired or credentials invalid. Sign in again." else detail ?: "Service unavailable (HTTP ${response.code}). Try again."
            response.close()
            throw java.io.IOException(message)
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private val retrofitBuilder: Retrofit.Builder
        get() = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())

    var apiService: ApiService = retrofitBuilder.build().create(ApiService::class.java)
        private set

    fun init(authManager: AuthManager) {
        this.authManager = authManager
        // Rebuild apiService with the interceptor aware of authManager
        apiService = retrofitBuilder.build().create(ApiService::class.java)
    }

    fun updateBaseUrl(newIp: String) {
        baseUrl = if (newIp.startsWith("http://") || newIp.startsWith("https://")) {
            if (newIp.endsWith("/")) newIp else "$newIp/"
        } else {
            "http://$newIp:8000/"
        }
        apiService = retrofitBuilder.build().create(ApiService::class.java)
    }
}
