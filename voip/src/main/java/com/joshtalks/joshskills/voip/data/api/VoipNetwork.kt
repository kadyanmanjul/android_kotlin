package com.joshtalks.joshskills.voip.data.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.joshtalks.joshskills.base.constants.KEY_APP_ACCEPT_LANGUAGE
import com.joshtalks.joshskills.base.constants.KEY_APP_USER_AGENT
import com.joshtalks.joshskills.base.constants.KEY_APP_VERSION_CODE
import com.joshtalks.joshskills.base.constants.KEY_APP_VERSION_NAME
import com.joshtalks.joshskills.base.constants.KEY_AUTHORIZATION
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.voipanalytics.data.network.VoipAnalyticsService
import okhttp3.*
import java.util.concurrent.TimeUnit
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http2.ConnectionShutdownException
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
            .retryOnConnectionFailure(false)
            .addInterceptor(HeaderInterceptor)

        if (BuildConfig.DEBUG) {
            val logging =
                HttpLoggingInterceptor { message ->
                    Timber.tag("OkHttp").d(message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            okHttpBuilder.addInterceptor(logging)
            okHttpBuilder.addNetworkInterceptor(StethoInterceptor())
        }

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getVoipApi(): CallingApiService {
        setup()
        return retrofit.create(CallingApiService::class.java)
    }

    fun getVoipAnalyticsApi(): VoipAnalyticsService {
        setup()
        return retrofit.create(VoipAnalyticsService::class.java)
    }
}

object HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val newRequest: Request.Builder = it.newBuilder()
            if (Utils.apiHeader?.token?.isNotEmpty() == true) {
                newRequest.addHeader(KEY_AUTHORIZATION, Utils.apiHeader?.token ?: "")
            }
            newRequest.addHeader(KEY_APP_VERSION_NAME, Utils.apiHeader?.versionName ?: "")
                .addHeader(KEY_APP_VERSION_CODE, Utils.apiHeader?.versionCode ?: "")
                .addHeader(KEY_APP_USER_AGENT, Utils.apiHeader?.userAgent ?: "")
                .addHeader(KEY_APP_ACCEPT_LANGUAGE, Utils.apiHeader?.acceptLanguage ?: "")
            chain.proceed(newRequest.build())
        }
    }
}

inline fun Request.safeCall(block: (Request) -> Response): Response {
    try {
        return block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        var msg = ""
        when (e) {
            is SocketTimeoutException -> {
                msg = "Timeout - Please check your internet connection"
            }
            is UnknownHostException -> {
                msg = "Unable to make a connection. Please check your internet"
            }
            is ConnectionShutdownException -> {
                msg = "Connection shutdown. Please check your internet"
            }
            is IOException -> {
                msg = "Server is unreachable, please try again later."
            }
            is IllegalStateException -> {
                msg = "${e.message}"
            }
            else -> {
                msg = "${e.message}"
            }
        }
        return Response.Builder()
            .request(this)
            .protocol(Protocol.HTTP_1_1)
            .code(999)
            .message(msg)
            .body("{${e}}".toResponseBody(null)).build()
    }
}