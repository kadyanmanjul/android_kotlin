package com.joshtalks.joshskills.quizgame.ui.data.network

import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class RetrofitInstance {
    companion object {
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
        private const val CONNECTION_TIMEOUT = 30L
        private const val CALL_TIMEOUT = 60L
        private const val cacheSize = 10 * 1024 * 1024.toLong()

        var api: GameApiService? = null
        fun getRetrofitInstance(): GameApiService? {
            try {
                val builder = OkHttpClient().newBuilder()
                    .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                    .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                    .followSslRedirects(true)
                    .addInterceptor(StatusCodeInterceptor())
                    .addInterceptor(HeaderInterceptor())
                    .hostnameVerifier { _, _ -> true }
                    .cache(cache())
                if (BuildConfig.DEBUG) {
                    builder.addInterceptor(getOkhhtpToolInterceptor())
                    val logging =
                        HttpLoggingInterceptor { message ->
                            Timber.tag("OkHttp").d(message)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    builder.addInterceptor(logging)
                    builder.addNetworkInterceptor(getStethoInterceptor())
                }
                val retrofit = Retrofit.Builder()
                    .baseUrl(Url.base_url)
                    .client(builder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                api = retrofit.create(GameApiService::class.java)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            return api
        }

        private fun cache(): Cache {
            return Cache(
                File(AppObjectController.joshApplication.cacheDir, "api_cache"),
                cacheSize
            )
        }
    }

}