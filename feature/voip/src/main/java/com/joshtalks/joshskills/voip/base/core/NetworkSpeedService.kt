package com.joshtalks.joshskills.voip.base.core

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@JvmSuppressWildcards
interface NetworkSpeedService {
    @GET
    @Headers("Connection: close")
    suspend fun downloadSpeedTestFile(@Url testFileUrl: String) : Response<ResponseBody>
}