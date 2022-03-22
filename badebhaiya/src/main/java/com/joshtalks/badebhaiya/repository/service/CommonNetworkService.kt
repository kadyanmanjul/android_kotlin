package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.repository.model.FCMData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface CommonNetworkService {

    @POST("$DIR/user/fcm/")
    suspend fun postFCMToken(@QueryMap params: Map<String, String>): Response<FCMData>

    @POST("$DIR/user/fcm/{id}")
    suspend fun patchFCMToken(
        @Path("id") userId: String,
        @Body params: Map<String, String>
    ): Response<FCMData>

}