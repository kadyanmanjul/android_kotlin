package com.joshtalks.joshskills.voip.data.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.joshtalks.joshskills.base.constants.KEY_APP_ACCEPT_LANGUAGE
import com.joshtalks.joshskills.base.constants.KEY_APP_USER_AGENT
import com.joshtalks.joshskills.base.constants.KEY_APP_VERSION_CODE
import com.joshtalks.joshskills.base.constants.KEY_APP_VERSION_NAME
import com.joshtalks.joshskills.base.constants.KEY_AUTHORIZATION
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.voipLog
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 30L
private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L

object VoipNetwork {
    lateinit var retrofit: Retrofit
    lateinit var okHttpBuilder: OkHttpClient.Builder

    private fun setup() {
        okHttpBuilder = OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                .followSslRedirects(true)

            if (BuildConfig.DEBUG) {
                val logging =
                    HttpLoggingInterceptor { message ->
                        Timber.tag("OkHttp").d(message)
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                okHttpBuilder.addInterceptor(HeaderInterceptor)
                okHttpBuilder.addInterceptor(logging)
                okHttpBuilder.addNetworkInterceptor(StethoInterceptor())
            }

        retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }

    fun getVoipApi()  : CallingApiService {
        setup()
        return retrofit.create(CallingApiService::class.java)
    }
}

object HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newRequest: Request.Builder = original.newBuilder()
        if (Utils.apiHeader?.token?.isNotEmpty() == true) {
            newRequest.addHeader(KEY_AUTHORIZATION, Utils.apiHeader?.token ?: "")
        }
        newRequest.addHeader(KEY_APP_VERSION_NAME, Utils.apiHeader?.versionName ?: "")
            .addHeader(KEY_APP_VERSION_CODE, Utils.apiHeader?.versionCode ?: "")
            .addHeader(KEY_APP_USER_AGENT, Utils.apiHeader?.userAgent ?: "")
            .addHeader(KEY_APP_ACCEPT_LANGUAGE, Utils.apiHeader?.acceptLanguage ?: "")

        return chain.proceed(newRequest.build())
    }
}