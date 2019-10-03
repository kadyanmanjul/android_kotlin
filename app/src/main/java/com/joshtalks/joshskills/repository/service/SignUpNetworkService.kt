package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality
import com.joshtalks.joshskills.repository.server.*
import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import retrofit2.http.*

const val DIR = "api/skill/v1"

interface SignUpNetworkService {

    @GET("$DIR/core/meta/")
    fun getCoreMeta(): Deferred<CoreMeta>

    @POST("$DIR/mentor/account_kit/")
    fun accountKitAuthorizationAsync(@Body accountKitRequest: AccountKitRequest): Deferred<CreateAccountResponse>


    @GET("$DIR/mentor/{id}/personal_profile/")
    fun getPersonalProfileAsync(@Path("id") id: String): Deferred<Mentor>


    @PATCH ("$DIR/mentor/{id}/")
    fun updateUserAddressAsync(@Path("id") id: String, @Body params: UpdateUserLocality): Deferred<ProfileResponse>


    @FormUrlEncoded
    @POST("$DIR/mentor/location/locality/")
    fun confirmUserLocationAsync(@FieldMap params: Map<String, String>): Deferred<Locality>

    @PATCH("$DIR/user/{id}/")
    fun updateUserAsync(@Path("id") id: String, @Body obj: UpdateUserPersonal): Deferred<UpdateProfileResponse>


    @PATCH("$DIR/mentor/{id}/last_login/")
    suspend fun userActive(@Path("id") id: String,@Body obj: Any)

    @Multipart
    @POST("$DIR/user/{id}/upload_profile_pic/")
    suspend fun uploadProfilePicture(@Path("id") id: String,@Part file: MultipartBody.Part): Any

    @POST("$DIR/mentor/devices/")
    fun updateDeviceDetails(@Body obj: UpdateDeviceRequest): Deferred<Any>

    @FormUrlEncoded
    @PATCH ("$DIR/mentor/fcm/{id}/")
    fun updateFCMToken(@Path("id") id: String, @FieldMap params: Map<String, String>): Deferred<Any>

    @FormUrlEncoded
    @POST("$DIR/mentor/fcm/")
    fun uploadFCMToken( @FieldMap params: Map<String, String>): Deferred<Any>

    @FormUrlEncoded
    @POST("$DIR/mentor/register/anonymous/")
    fun registerAnonymousUser(@FieldMap params: Map<String, String>): Deferred<SuccessResponse>





}