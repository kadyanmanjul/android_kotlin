package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality
import com.joshtalks.joshskills.repository.server.*
import kotlinx.coroutines.Deferred
import retrofit2.http.*

const val DIR="api/skill/v1"

interface JoshNetworkService {

    @GET("$DIR/core/meta/")
    fun getCoreMeta(): Deferred<CoreMeta>

    @POST("$DIR/mentor/account_kit/")
    fun accountKitAuthorizationAsync(@Body accountKitRequest: AccountKitRequest): Deferred<CreateAccountResponse>


    @GET("$DIR/mentor/{id}/personal_profile/")
    fun getPersonalProfileAsync(@Path("id") id: String): Deferred<Mentor>

    @FormUrlEncoded
    @POST("$DIR/mentor/location/locality/")
    fun confirmUserLocationAsync(@FieldMap params: Map<String, String>): Deferred<Locality>

    @PATCH("$DIR/user/{id}/")
    fun updateUserAsync(@Path("id") id: String, @Body obj: UpdateUserPersonal): Deferred<UpdateProfileResponse>


    @PATCH("$DIR/user/{id}/")
    fun updateUserAddressAsync(@Path("id") id: String, @Body obj: UpdateUserLocality): Deferred<UpdateProfileResponse>


}