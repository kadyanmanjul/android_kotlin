package com.joshtalks.joshskills.di.module

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.core.Envelope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.di.ApplicationEvent
import com.joshtalks.joshskills.di.ApplicationEventListener
import com.joshtalks.joshskills.di.annotation.AppScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

private const val JOSH_SKILLS_CACHE = "joshskills-cache"
private const val DEFAULT_READ_TIMEOUT = 30
private const val DEFAULT_WRITE_TIMEOUT = 30
private const val DEFAULT_CONNECTION_TIMEOUT = 30
private const val CALL_TIMEOUT = 60L
private const val CACHE_SIZE = 10 * 1024 * 1024L

@Module
class NetworkModule {
    @AppScope
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, gsonConverterFactory: GsonConverterFactory, coroutineCallAdapterFactory: CoroutineCallAdapterFactory) : Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addCallAdapterFactory(coroutineCallAdapterFactory)
            .addConverterFactory(gsonConverterFactory)
            .build()
    }

    @AppScope
    @Provides
    fun provideCoroutineCallAdapterFactory(@Named("Gson") gson: Gson) : CoroutineCallAdapterFactory {
        return CoroutineCallAdapterFactory()
    }

    @AppScope
    @Provides
    fun provideGsonConverterFactory(@Named("Gson") gson: Gson) : GsonConverterFactory {
        return GsonConverterFactory.create(gson)
    }

    @AppScope
    @Provides
    @Named("Gson")
    fun provideNetworkGson() : Gson {
        return GsonBuilder()
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
    }

    @AppScope
    @Provides
    @Named("Local Gson")
    fun provideLocalGson() : Gson {
        return GsonBuilder()
            .serializeNulls()
            .setDateFormat(DateFormat.LONG)
            .setPrettyPrinting()
            .setLenient()
            .create()
    }

    @AppScope
    @Provides
    fun provideOkHttp(headerInterceptor: HeaderInterceptor, statusCodeInterceptor: StatusCodeInterceptor, timeoutInterceptor: TimeoutInterceptor, cache: Cache) : OkHttpClient {
        val builder = OkHttpClient().newBuilder()
            .connectTimeout(DEFAULT_CONNECTION_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
            // .retryOnConnectionFailure(true)
            .followSslRedirects(true)
            .addInterceptor(statusCodeInterceptor)
            .addInterceptor(headerInterceptor)
            .addInterceptor(timeoutInterceptor)
            .hostnameVerifier { _, _ -> true }
            .cache(cache)
        //TODO: Need below code for debugging
//        if (BuildConfig.DEBUG) {
//            builder.addInterceptor(getOkhhtpToolInterceptor())
//            val logging =
//                HttpLoggingInterceptor { message ->
//                    Timber.tag("OkHttp").d(message)
//                }.apply {
//                    level = HttpLoggingInterceptor.Level.BODY
//
//                }
//            builder.addInterceptor(logging)
//            builder.addNetworkInterceptor(getStethoInterceptor())
//            builder.eventListener(PrintingEventListener())
//        }
        return builder.build()
    }

//    @AppScope
//    @Provides
//    @Named("HeaderInterceptor")
//    fun getOkhhtpToolInterceptor(): Interceptor {
//        val clazz = Class.forName("com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor")
//        val ctor: Constructor<*> = clazz.getConstructor()
//        return ctor.newInstance() as Interceptor
//    }

    @AppScope
    @Provides
    fun provideCache(application : Application) : Cache {
        return Cache(
            File(application.cacheDir, "api_cache"),
            CACHE_SIZE
        )
    }
}

inline fun Request.safeCall(block: (Request) -> Response): Response {
    try {
        return block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            FirebaseCrashlytics.getInstance().log(this.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        if (e is IOException) {
            val msg = "Unable to make a connection. Please check your internet"
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

@AppScope
class HeaderInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val newRequest: Request.Builder = it.newBuilder()
            if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
                newRequest.addHeader(
                    KEY_AUTHORIZATION, "JWT " + PrefManager.getStringValue(API_TOKEN)
                )
            }
            newRequest.addHeader(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                .addHeader(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
                .addHeader(
                    KEY_APP_USER_AGENT,
                    "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString()
                )
                .addHeader(KEY_APP_ACCEPT_LANGUAGE, PrefManager.getStringValue(USER_LOCALE))
            if (Utils.isInternetAvailable()) {
                newRequest.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                if (it.headers.none().not()) {
                    newRequest.cacheControl(CacheControl.FORCE_CACHE)
                }
            }
            chain.proceed(newRequest.build())
        }
    }
}

/**
 * Use below annotations for dynamic timeout
 * @Headers({"CONNECT_TIMEOUT:10000", "READ_TIMEOUT:10000", "WRITE_TIMEOUT:10000"})
 */
@AppScope
class TimeoutInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val connectionTimeout = it.header(CONNECTION_TIMEOUT)?.toIntOrNull()
            if(connectionTimeout != null) {
                chain
                    .withConnectTimeout(connectionTimeout, TimeUnit.SECONDS)
                    .withReadTimeout(it.header(READ_TIMEOUT)?.toIntOrNull() ?: DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                    .withWriteTimeout(it.header(WRITE_TIMEOUT)?.toIntOrNull() ?: DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS)
                    .proceed(it);
            } else {
                chain.proceed(it)
            }
        }
    }
}

@AppScope
class StatusCodeInterceptor @Inject constructor(val scope : CoroutineScope, val channel : ApplicationEventListener) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request().safeCall {
            val response = chain.proceed(it)
            if (response.code in 401..403) {
                scope.launch { channel.emitEvent(Envelope(ApplicationEvent.UNAUTHORISED)) }
            }
            Timber.i("Status code: %s", response.code)
            response
        }
    }
}