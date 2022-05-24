package com.joshtalks.badebhaiya.repository.service

import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import com.joshtalks.badebhaiya.signup.response.BBtoFollow
import com.joshtalks.badebhaiya.signup.response.LoginResponse
import com.joshtalks.badebhaiya.signup.response.OTPResponse
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

const val DIR = "api/bbapp/v1"
interface SignUpNetworkService {

    @GET("$DIR/user/login/")
    suspend fun sendNumberForOTP(@QueryMap params: Map<String, String>):Response<OTPResponse>

    @POST("$DIR/user/verify_otp/")
    suspend fun verityOTP(@Body verifyOTPRequest: VerifyOTPRequest): Response<LoginResponse>

    @GET("$DIR/user/{id}/")
    suspend fun getUserProfile(@Path("id")userId: String): Response<User>

    @GET("$DIR/user/sign_out/")
    suspend fun signOutUser():Response<Void>

    @PATCH("$DIR/user/{id}/")
    suspend fun updateUserProfile(@Path("id")userId: String, @Body params: Map<String, String?>): Response<User>

    @POST("$DIR/user/truecaller_login/")
    suspend fun trueCallerLogin(@Body params: Map<String, String>) : Response<LoginResponse>

    @GET("$DIR/user/speakers_to_follow/")
    suspend fun speakersList(
        @Query("page") page: Int
    ):Response<BBtoFollow>

}