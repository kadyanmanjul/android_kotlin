package com.joshtalks.joshskills.base.core

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface NetworkSpeedService {
    @GET("www.static.skills.com/speed_test.jpg")
    suspend fun downloadSpeedTestFile() : Response<ResponseBody>
}