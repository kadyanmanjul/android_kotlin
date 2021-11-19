package com.joshtalks.joshskills.quizgame.ui.data.network

import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory

import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


class RetrofitInstanse {
    companion object{
//        var okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
//            .connectTimeout(60, TimeUnit.SECONDS)
//            .readTimeout(60, TimeUnit.SECONDS)
//            .writeTimeout(60, TimeUnit.SECONDS)
//            .build()
        private val retrofit=Retrofit.Builder()
            .baseUrl(Url.base_url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api:Api= retrofit.create(Api::class.java)
    }
//    private var retrofit: Retrofit? = null
//
//     fun getRetrofitInstance(): Retrofit? {
//        if (retrofit == null) {
//            retrofit = Retrofit.Builder()
//                .baseUrl(Url.base_url)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//        }
//        return retrofit
//    }
}