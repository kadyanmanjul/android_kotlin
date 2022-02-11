package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.GameBaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.quizgame.ui.data.repository.RandomTeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class RandomTeamMateFoundViewModelGame(var randomTeamMateVmApplication: Application) : GameBaseViewModel(randomTeamMateVmApplication) {

    val randomTeamMateFoundRepo = RandomTeamMateFoundRepo(RetrofitInstance.getRetrofitInstance())
    val userData: MutableLiveData<UserDetails> = MutableLiveData()

    val clearRadius: MutableLiveData<Success> = MutableLiveData()

    val saveCallDuration: MutableLiveData<CallDurationResponse> = MutableLiveData()


    fun getChannelData(mentorId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = randomTeamMateFoundRepo.getUserDetails(mentorId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        userData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun getClearRadius(randomRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = randomTeamMateFoundRepo.clearRoomRadius(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        clearRadius.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun saveCallDuration(callDuration: SaveCallDuration) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = randomTeamMateFoundRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }
}