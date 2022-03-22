package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import com.joshtalks.badebhaiya.signup.response.LoginResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

const val DIR = "api/bbapp/v1"
interface SignUpNetworkService {

    @GET("$DIR/user/login/")
    suspend fun sendNumberForOTP(@QueryMap params: Map<String, String>)

    @POST("$DIR/user/verify_otp/")
    suspend fun verityOTP(@Body verifyOTPRequest: VerifyOTPRequest): Response<LoginResponse>

    @GET("$DIR/user/{id}/")
    suspend fun getUserProfile(@Path("id")userId: String): Response<User>

    @FormUrlEncoded
    @PATCH("$DIR/user/{id}/")
    suspend fun updateUserProfile(@Path("id")userId: String, @FieldMap params: Map<String, String?>): Response<User>

    @FormUrlEncoded
    @POST("$DIR/core/signed_url/")
    suspend fun requestUploadMedia(requestParams: Map<String, String>): Deferred<AmazonPolicyResponse>
}