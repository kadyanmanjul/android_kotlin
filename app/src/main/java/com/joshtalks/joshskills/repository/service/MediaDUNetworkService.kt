package com.joshtalks.joshskills.repository.service

import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Url


@JvmSuppressWildcards
interface MediaDUNetworkService {

    @GET
    fun downloadFileAsync(@Url fileUrl: String): Deferred<Any>


    @Multipart
    @POST
    fun uploadMediaAsync(
        @Url url: String,
        @PartMap messageObject: Map<String, RequestBody>,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

}