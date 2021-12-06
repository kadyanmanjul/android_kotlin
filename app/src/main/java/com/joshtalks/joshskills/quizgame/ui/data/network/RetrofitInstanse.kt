package com.joshtalks.joshskills.quizgame.ui.data.network

import android.util.Log
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class RetrofitInstanse {
    companion object{
        private const val JOSH_SKILLS_CACHE = "joshskills-cache"
        private const val READ_TIMEOUT = 30L
        private const val WRITE_TIMEOUT = 30L
        private const val CONNECTION_TIMEOUT = 30L
        private const val CALL_TIMEOUT = 60L
        private const val cacheSize = 10 * 1024 * 1024.toLong()

        var api:Api?=null
       // private lateinit var retrofit: Retrofit
        fun getRetrofitInstance(): Api? {
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
                api = retrofit.create(Api::class.java)
            }catch (ex:Exception){
                Log.d("error_res", "getRetrofitInstance: "+ex.message)
            }
            return api
        }

        private fun cache(): Cache? {
            return Cache(
                File(AppObjectController.joshApplication.cacheDir, "api_cache"),
                cacheSize
            )
        }
//        private val retrofit = Retrofit.Builder()
//            .baseUrl(Url.base_url)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
    }

}