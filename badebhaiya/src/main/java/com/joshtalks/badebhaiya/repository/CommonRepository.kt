package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest

class CommonRepository {

    private val service = RetrofitInstance.commonNetworkService
    suspend fun postFCMToken(requestParams: Map<String, String>) = service.sendNumberForOTP(requestParams)
    suspend fun patchFCMToken(verifyOTPRequest: VerifyOTPRequest) = service.verityOTP(verifyOTPRequest)
    suspend fun requestUploadMediaAsync(requestParams: Map<String, String>) = service.requestUploadMedia(requestParams)
}