package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.core.models.DeviceDetailsResponse
import com.joshtalks.badebhaiya.core.models.InstallReferrerModel
import com.joshtalks.badebhaiya.core.models.UpdateDeviceRequest
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CommonNetworkService {

    @POST("$DIR/user/fcm/")
    suspend fun postFCMToken(@Body params: Map<String, String>): Response<FCMData>

    @PATCH("$DIR/user/fcm/{id}/")
    suspend fun patchFCMToken(
        @Path("id") id: Int,
        @Body params: Map<String, String>
    ): Response<FCMData>

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap requestParams: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/user/source/")
    suspend fun getInstallReferrerAsync(requestParams: InstallReferrerModel): Response<InstallReferrerModel>

    @POST("$DIR/user/devices/")
    suspend fun postDeviceDetails(@Body obj: UpdateDeviceRequest): DeviceDetailsResponse

    @PATCH("$DIR/user/devices/{device_id}/")
    suspend fun patchDeviceDetails( @Path("device_id") deviceId: Int, @Body obj: UpdateDeviceRequest): DeviceDetailsResponse
}