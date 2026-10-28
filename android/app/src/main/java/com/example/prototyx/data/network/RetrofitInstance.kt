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
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
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
