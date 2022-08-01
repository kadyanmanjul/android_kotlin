package com.joshtalks.joshskills.repository.service

import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


@JvmSuppressWildcards
interface MediaDUNetworkService {

    @GET
    @Headers("CONNECT_TIMEOUT:45", "READ_TIMEOUT:45", "WRITE_TIMEOUT:45")
    fun downloadFileAsync(@Url fileUrl: String): Deferred<Any>


    @Multipart
    @POST
    @Headers("CONNECT_TIMEOUT:45", "READ_TIMEOUT:45", "WRITE_TIMEOUT:45")
    fun uploadMediaAsync(
        @Url url: String,
        @PartMap messageObject: Map<String, RequestBody>,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>
}