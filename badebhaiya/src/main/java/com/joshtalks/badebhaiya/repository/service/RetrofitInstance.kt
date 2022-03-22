package com.joshtalks.badebhaiya.repository.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.core.API_TOKEN
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.PrefManager
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val KEY_AUTHORIZATION = "Authorization"
private const val READ_TIMEOUT = 30L
private const val WRITE_TIMEOUT = 30L
private const val CONNECTION_TIMEOUT = 30L
private const val CALL_TIMEOUT = 60L
class RetrofitInstance {

    companion object {
        @JvmStatic
        lateinit var gsonMapper: Gson

        @JvmStatic
        lateinit var gsonMapperForLocal: Gson

        private const val cacheSize = 10 * 1024 * 1024.toLong()

        private val retrofit by lazy {
            gsonMapper = GsonBuilder()
                .enableComplexMapKeySerialization()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .registerTypeAdapter(Date::class.java, object : JsonDeserializer<Date> {
                    @Throws(JsonParseException::class)
                    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date {
                        return Date(json.asJsonPrimitive.asLong * 1000)
                    }
                })
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .serializeNulls()
                .create()

            gsonMapperForLocal = GsonBuilder()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .setLenient()
                .create()

            val builder = OkHttpClient().newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(CALL_TIMEOUT, TimeUnit.SECONDS)
                .followSslRedirects(true)
//                .addInterceptor(StatusCodeInterceptor())
                .addInterceptor(HeaderInterceptor())
                .hostnameVerifier { _, _ -> true }
                .cache(cache())

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(getOkhttpToolInterceptor())
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                builder.addInterceptor(logging)
                builder.addNetworkInterceptor(getStethoInterceptor())
            }
            Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(builder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()
        }

        private fun cache() = Cache(File(AppObjectController.joshApplication.cacheDir, "api_cache"), cacheSize)

        val signUpNetworkService by lazy {
            retrofit.create(SignUpNetworkService::class.java)
        }

        val commonNetworkService by lazy {
            retrofit.create(CommonNetworkService::class.java)
        }
    }
}

class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val newRequest: Request.Builder = original.newBuilder()
        if (PrefManager.getStringValue(API_TOKEN).isNotEmpty()) {
            newRequest.addHeader(KEY_AUTHORIZATION, "JWT " + PrefManager.getStringValue(API_TOKEN))
        }
        return chain.proceed(newRequest.build())
    }
}

fun initStethoLibrary(context: Context) {
    val cls = Class.forName("com.facebook.stetho.Stetho")
    val m: Method = cls.getMethod("initializeWithDefaults", Context::class.java)
    m.invoke(null, context)
}

fun getStethoInterceptor(): Interceptor {
    val clazz = Class.forName("com.facebook.stetho.okhttp3.StethoInterceptor")
    val ctor: Constructor<*> = clazz.getConstructor()
    return ctor.newInstance() as Interceptor
}

fun getOkhttpToolInterceptor(): Interceptor {
    val clazz = Class.forName("com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor")
    val ctor: Constructor<*> = clazz.getConstructor()
    return ctor.newInstance() as Interceptor
}