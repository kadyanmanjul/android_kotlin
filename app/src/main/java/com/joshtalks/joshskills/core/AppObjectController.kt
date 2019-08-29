package com.joshtalks.joshskills.core

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.service.JoshNetworkService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import java.text.DateFormat


val KEY_AUTHORIZATION = "Authorization"
val KEY_APP_VERSION_CODE = "app-version-code"
val KEY_APP_VERSION_NAME = "app-version-name"
val REMOTE_CONFIG_PREFIX = "josh_param_"

internal class AppObjectController {

    companion object {

        @JvmStatic
        var INSTANCE: AppObjectController =
            AppObjectController()


        @JvmStatic
        lateinit var joshApplication: JoshApplication
            private set
        @JvmStatic
        lateinit var appDatabase: AppDatabase
            private set

        @JvmStatic
        lateinit var gsonMapper: Gson
            private set

        @JvmStatic
        lateinit var retrofit: Retrofit

        @JvmStatic
        lateinit var networkService: JoshNetworkService


        fun init(context: JoshApplication): AppObjectController {
            joshApplication = context;
            appDatabase = AppDatabase.getDatabase(context)!!

            gsonMapper = GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setDateFormat(DateFormat.LONG)
                .setPrettyPrinting()
                .create()


            var builder = OkHttpClient().newBuilder()
            if (BuildConfig.DEBUG) {
                builder.addNetworkInterceptor(StethoInterceptor())
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
                builder.addInterceptor(logging)
            }
            builder.addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val original = chain.request()
                    val newRequest: Request.Builder = original.newBuilder()
                    if (User.getInstance() != null && User.getInstance().token != null) {
                        newRequest.addHeader(
                            KEY_AUTHORIZATION,
                            "Bearer " + (User.getInstance().token?.accessToken ?: "")
                        )
                            .addHeader(KEY_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                            .addHeader(KEY_APP_VERSION_CODE, BuildConfig.VERSION_CODE.toString())
                    }
                    return chain.proceed(newRequest.build())
                }

            })

            //https://live.joshtalks.org/
            retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.14.39:8000")
                .client(builder.build())
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create(gsonMapper))
                .build()


            networkService = retrofit.create(JoshNetworkService::class.java)

            //Set emoji for this project
            // val config = BundledEmojiCompatConfig(context)
            //config.setReplaceAll(true)
            //var emojiCompat: EmojiCompat = EmojiCompat.init(config)
            // EmojiManager.install(GoogleCompatEmojiProvider(emojiCompat))


            return INSTANCE
        }


    }

}