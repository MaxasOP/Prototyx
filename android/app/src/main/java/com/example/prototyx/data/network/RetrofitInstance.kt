package com.example.prototyx.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    // Production URL on Render
    private var baseUrl = "https://prototyx.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofitBuilder: Retrofit.Builder
        get() = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())

    var apiService: ApiService = retrofitBuilder.build().create(ApiService::class.java)
        private set

    /**
     * Updates the base URL (useful if running on a physical phone connecting to the computer's local IP on Wi-Fi).
     */
    fun updateBaseUrl(newIp: String) {
        baseUrl = if (newIp.startsWith("http://") || newIp.startsWith("https://")) {
            if (newIp.endsWith("/")) newIp else "$newIp/"
        } else {
            "http://$newIp:8000/"
        }
        apiService = retrofitBuilder.build().create(ApiService::class.java)
    }
}
