package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest

class BBRepository {

    private val service = RetrofitInstance.signUpNetworkService
    suspend fun sendPhoneNumberForOTP(requestParams: Map<String, String>) = service.sendNumberForOTP(requestParams)

    suspend fun verifyOTP(verifyOTPRequest: VerifyOTPRequest) = service.verityOTP(verifyOTPRequest)
    suspend fun getUserDetailsForSignUp(userId: String) = service.getUserProfile(userId)
    suspend fun updateUserProfile(userId: String, requestMap: MutableMap<String, String?>) = service.updateUserProfile(userId, requestMap)
}