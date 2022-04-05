package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import com.joshtalks.badebhaiya.signup.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body

class BBRepository {

    private val service = RetrofitInstance.signUpNetworkService
    suspend fun sendPhoneNumberForOTP(requestParams: Map<String, String>) = service.sendNumberForOTP(requestParams)

    /*suspend fun trueCallerLogin(params: Map<String, String>) : Response<LoginResponse>{
        return service.trueCallerLogin(params)
    }*/
    suspend fun trueCallerLogin(params:Map<String,String>)= service.trueCallerLogin(params)
    suspend fun verifyOTP(verifyOTPRequest: VerifyOTPRequest) = service.verityOTP(verifyOTPRequest)
    suspend fun getUserDetailsForSignUp(userId: String) = service.getUserProfile(userId)
    suspend fun updateUserProfile(userId: String, requestMap: MutableMap<String, String?>) = service.updateUserProfile(userId, requestMap)
    suspend fun getProfileForUser(userId: String) = RetrofitInstance.profileNetworkService.getProfileForUser(userId)
}