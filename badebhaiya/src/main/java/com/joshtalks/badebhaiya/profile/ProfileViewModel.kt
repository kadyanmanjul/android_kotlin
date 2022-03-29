package com.joshtalks.badebhaiya.profile

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import kotlinx.coroutines.launch

class ProfileViewModel: ViewModel() {
    val userIdForOpenedProfile = MutableLiveData<String>()
    private val service = RetrofitInstance.profileNetworkService
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    val repository = BBRepository()
    val userProfileData = MutableLiveData<ProfileResponse>()

    fun updateFollowStatus() {
        viewModelScope.launch {
            try {
                val followRequest =
                    FollowRequest(userIdForOpenedProfile.value ?: "", User.getInstance().userId)
                val response = service.updateFollowStatus(followRequest)
                if (response.isSuccessful) {

                }
            } catch (ex: Exception) {

            }
        }
    }

    fun getProfileForUser(userId: String) {
        viewModelScope.launch {
            try {
                userIdForOpenedProfile.postValue(userId)
                val response = repository.getProfileForUser(userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        userProfileData.postValue(it)
                        isBadeBhaiyaSpeaker.set(it.isSpeaker)
                    }
                }
            } catch(ex: Exception) {

            }
        }
    }
}