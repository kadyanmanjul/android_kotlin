package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.quizgame.base.GameBaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.AddFavouritePartner
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToTokenResponse
import com.joshtalks.joshskills.quizgame.ui.data.model.Success
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.quizgame.ui.data.repository.ChoiceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChoiceViewModelGame(var choiceViewModelApplication: Application) : GameBaseViewModel(choiceViewModelApplication) {
    val choiceRepo = ChoiceRepo(RetrofitInstance.getRetrofitInstance())
    val statusResponse: MutableLiveData<Success> = MutableLiveData()
    val homeInactiveResponse: MutableLiveData<Success> = MutableLiveData()
    val agoraToToken: MutableLiveData<AgoraToTokenResponse> = MutableLiveData()

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