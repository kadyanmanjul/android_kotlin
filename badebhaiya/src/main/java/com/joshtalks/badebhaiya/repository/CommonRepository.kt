package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class CommonRepository {

    private val service = RetrofitInstance.commonNetworkService

    suspend fun requestUploadMediaAsync(requestParams: Map<String, String>) =
        service.requestUploadMedia(requestParams)

    suspend fun postFCMToken(requestParams: Map<String, String>) =
        service.postFCMToken(requestParams)

    suspend fun patchFCMToken(userId: String, requestParams: Map<String, String>) =
        service.patchFCMToken(userId, requestParams)

}