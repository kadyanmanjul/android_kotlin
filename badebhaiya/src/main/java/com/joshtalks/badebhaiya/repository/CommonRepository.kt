package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.core.models.InstallReferrerModel
import com.joshtalks.badebhaiya.core.models.UpdateDeviceRequest
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

class CommonRepository {

    private val service = RetrofitInstance.commonNetworkService

    suspend fun postFCMToken(requestParams: Map<String, String>) =
        service.postFCMToken(requestParams)

    suspend fun patchFCMToken(userId: String, requestParams: Map<String, String>) =
        service.patchFCMToken(userId, requestParams)

    suspend fun requestUploadMediaAsync(requestParams: Map<String, String>) =
        service.requestUploadMedia(requestParams)

    suspend fun getInstallReferrerAsync(obj: InstallReferrerModel) =
        service.getInstallReferrerAsync(obj)

    suspend fun postDeviceDetails(obj: UpdateDeviceRequest) =
        service.postDeviceDetails(obj)

    suspend fun patchDeviceDetails(deviceId: Int,obj: UpdateDeviceRequest) =
        service.patchDeviceDetails(deviceId,obj)
}