package com.joshtalks.joshskills.voip.data.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.voipanalytics.data.network.VoipAnalyticsService
import okhttp3.*
import java.util.concurrent.TimeUnit
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.net.UnknownHostException
import java.text.DateFormat
import java.util.*

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

        val gsonMapper = GsonBuilder()
            .enableComplexMapKeySerialization()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
                @Throws(JsonParseException::class)
                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type,
                    context: JsonDeserializationContext
                ): Date {
                    return Date(json.asJsonPrimitive.asLong * 1000)
                }
            })
            .excludeFieldsWithModifiers(
                Modifier.TRANSIENT
            )
            .setDateFormat(DateFormat.LONG)
            .setPrettyPrinting()
            .serializeNulls()
            .create()

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpBuilder.build())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gsonMapper))
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
            val response = chain.proceed(newRequest.build())
            if(response.code == 403) {
                val newResponse = response.newBuilder().code(203).message(response.message)
                newResponse.build()
            } else
                response
        }
    }
}

inline fun Request.safeCall(block: (Request) -> Response): Response {
    try {
        return block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        var msg = ""
        if(e is UnknownHostException) {
            return Response.Builder()
                .request(this)
                .protocol(Protocol.HTTP_1_1)
                .code(999)
                .message(msg)
                .body("{${e}}".toResponseBody(null)).build()
        }
        throw e
    }
}