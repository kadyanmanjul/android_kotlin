package com.joshtalks.joshskills.base.core

import okhttp3.*
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit

private const val READ_TIMEOUT = 15L
private const val WRITE_TIMEOUT = 15L
private const val CONNECTION_TIMEOUT = 15L
private const val CALL_TIMEOUT = 30L

object RetrofitNetwork {
    lateinit var retrofit: Retrofit
    lateinit var okHttpBuilder: OkHttpClient.Builder

    init {
        setup()
    }

    private fun setup() {
        okHttpBuilder = OkHttpClient().newBuilder()
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
            .followSslRedirects(true)
            .retryOnConnectionFailure(false)

        retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/")
            .client(okHttpBuilder.build())
            .build()
    }

    fun getNetworkSpeedApi(): NetworkSpeedService {
        return retrofit.create(NetworkSpeedService::class.java)
    }
}