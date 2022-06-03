package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.core.models.DeviceDetailsResponse
import com.joshtalks.badebhaiya.core.models.FormResponse
import com.joshtalks.badebhaiya.core.models.InstallReferrerModel
import com.joshtalks.badebhaiya.core.models.UpdateDeviceRequest
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

interface CommonNetworkService {

    @POST("$DIR/user/fcm/")
    suspend fun postFCMToken(@Body params: Map<String, String>): Response<FCMData>

    @GET("$DIR/user/sign_out/")
    suspend fun signOutUser(): Response<Void>

    @POST("$DIR/user/fcm_verify/")
    suspend fun checkFCMInServer(@Body params:Map<String,String>):Map<String,String>

    @PATCH("$DIR/user/fcm/{id}/")
    suspend fun patchFCMToken(
        @Path("id") id: Int,
        @Body params: Map<String, String>
    ): Response<FCMData>

    @POST("$DIR/user/reminder_form_response/")
    suspend fun sendMsg(@Body params: FormResponse):Response<Void>

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    fun requestUploadMediaAsync(@FieldMap requestParams: Map<String, String>): Deferred<AmazonPolicyResponse>

    @POST("$DIR/user/source/")
    suspend fun getInstallReferrerAsync(@Body requestParams: InstallReferrerModel): Response<InstallReferrerModel>

    @POST("$DIR/user/devices/")
    suspend fun postDeviceDetails(@Body obj: UpdateDeviceRequest): DeviceDetailsResponse

    @PATCH("$DIR/user/devices/{device_id}/")
    suspend fun patchDeviceDetails( @Path("device_id") deviceId: Int, @Body obj: UpdateDeviceRequest): DeviceDetailsResponse


}