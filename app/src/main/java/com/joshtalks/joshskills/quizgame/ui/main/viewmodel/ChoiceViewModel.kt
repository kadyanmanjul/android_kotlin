package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.ui.data.model.AgoraToTokenResponse
import com.joshtalks.joshskills.quizgame.ui.data.model.Success
import com.joshtalks.joshskills.quizgame.ui.data.repository.ChoiceRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ChoiceViewModel(var application111: Application) : AndroidViewModel(application111) {
    val choiceRepo = ChoiceRepo()
    val statusResponse: MutableLiveData<Success> = MutableLiveData()
    val homeInactiveResponse: MutableLiveData<Success> = MutableLiveData()
    val agoraToToken: MutableLiveData<AgoraToTokenResponse> = MutableLiveData()

    fun statusChange(userIdMentor: String?, status: String?) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
}