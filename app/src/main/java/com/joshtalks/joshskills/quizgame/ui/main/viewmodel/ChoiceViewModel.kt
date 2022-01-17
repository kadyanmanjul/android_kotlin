package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToTokenResponse
import com.joshtalks.joshskills.quizgame.ui.data.model.Success
import com.joshtalks.joshskills.quizgame.ui.data.repository.ChoiceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChoiceViewModel(var application11: Application) : BaseViewModel(application11) {
    val choiceRepo = ChoiceRepo()
    val statusResponse: MutableLiveData<Success> = MutableLiveData()
    val homeInactiveResponse: MutableLiveData<Success> = MutableLiveData()
    val agoraToToken: MutableLiveData<AgoraToTokenResponse> = MutableLiveData()

//    fun openFavouritePartnerScreen(){
//        message.what = OPEN_FAVOURITE_SCREEN
//        singleLiveEvent.value = message
//    }
//
//    fun openRandomScreen(){
//        message.what = OPEN_RANDOM_SCREEN
//        singleLiveEvent.value = message
//    }
    fun statusChange(userIdMentor: String?, status: String?) {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = choiceRepo.getStatus(userIdMentor, status)
                    if (response?.isSuccessful == true && response.body() != null) {
                        statusResponse.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            showToast(ex.message?:"")
        }
    }

    fun homeInactive(userIdMentor: String?, status: String?) {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = choiceRepo.getHomeInactive(userIdMentor, status)
                    if (response?.isSuccessful == true && response.body() != null) {
                        homeInactiveResponse.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            showToast(ex.message?:"")
        }
    }

    fun getChannelData(agoraToId: String?, channelName: String?) {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = choiceRepo.getChannelData(agoraToId, channelName)
                    if (response?.isSuccessful == true && response.body() != null) {
                        agoraToToken.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            showToast(ex.message?:"")
        }
    }

    fun addFavouritePracticePartner(addFavouritePartner: AddFavouritePartner) {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = choiceRepo.addFavouritePartner(addFavouritePartner)
                }
            }
        } catch (ex: Exception) {

        }
    }
}