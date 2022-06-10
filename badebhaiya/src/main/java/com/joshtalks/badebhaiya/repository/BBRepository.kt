package com.joshtalks.badebhaiya.repository

import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.peopleToFollow.PeoplePagingSource
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import retrofit2.Response

class BBRepository {

    private val service = RetrofitInstance.signUpNetworkService
    suspend fun sendPhoneNumberForOTP(requestParams: Map<String, String>) = service.sendNumberForOTP(requestParams)
    suspend fun verifyOTP(verifyOTPRequest: VerifyOTPRequest) = service.verityOTP(verifyOTPRequest)
    suspend fun getUserDetailsForSignUp(userId: String) = service.getUserProfile(userId)
    suspend fun updateUserProfile(userId: String, requestMap: MutableMap<String, String?>) =
        service.updateUserProfile(userId, requestMap)

    suspend fun getProfileForUser(userId: String, source: String): Response<ProfileResponse> {
        return if (User.getInstance().isLoggedIn()) {
            RetrofitInstance.profileNetworkService.getProfileForUser(userId, source)
        } else {
            RetrofitInstance.profileNetworkService.getProfileWithoutToken(userId, source)
        }
    }

    suspend fun signOut() = service.signOutUser()

    suspend fun trueCallerLogin(params: Map<String, String>) = service.trueCallerLogin(params)

    suspend fun getBBtoFollowList(page: Int) = service.speakersList(page)

    fun bbToFollowPaginatedList() = PeoplePagingSource()
}