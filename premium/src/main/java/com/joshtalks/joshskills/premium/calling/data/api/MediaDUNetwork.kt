package com.joshtalks.joshskills.premium.calling.data.api

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.premium.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.lang.reflect.Constructor
import java.util.concurrent.TimeUnit

object MediaDUNetwork {
    lateinit var retrofit: Retrofit
    lateinit var mediaOkhttpBuilder: OkHttpClient.Builder

    private fun setup() {

        mediaOkhttpBuilder = OkHttpClient().newBuilder()
        mediaOkhttpBuilder.connectTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)

        if (BuildConfig.DEBUG) {
            val logging =
                HttpLoggingInterceptor { message ->
                    Timber.tag("OkHttp").d(message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BODY

                }
            mediaOkhttpBuilder.addInterceptor(logging)
            mediaOkhttpBuilder.addNetworkInterceptor(getStethoInterceptor())
            mediaOkhttpBuilder.addInterceptor(getOkhttpToolInterceptor())
        }

        mediaOkhttpBuilder.addInterceptor(Interceptor { chain ->
            chain.request().safeCall {
                val newRequest: Request.Builder = it.newBuilder()
                newRequest.addHeader("Connection", "close")
                chain.proceed(newRequest.build())
            }
        })

        retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(mediaOkhttpBuilder.build())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    fun getMediaDUNetworkService(): MediaDUNetworkService {
        setup()
        return retrofit.create(MediaDUNetworkService::class.java)
    }

    private fun getStethoInterceptor(): Interceptor {
        val clazz = Class.forName("com.facebook.stetho.okhttp3.StethoInterceptor")
        val ctor: Constructor<*> = clazz.getConstructor()
        return ctor.newInstance() as Interceptor
    }

    private fun getOkhttpToolInterceptor(): Interceptor {
        val clazz = Class.forName("com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor")
        val ctor: Constructor<*> = clazz.getConstructor()
        return ctor.newInstance() as Interceptor
    }
}