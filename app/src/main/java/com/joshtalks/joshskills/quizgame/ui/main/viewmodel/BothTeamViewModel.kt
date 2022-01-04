package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BothTeamViewModel(var application11: Application) : BaseViewModel(application11) {
    val bothTeamRepo = BothTeamRepo()
    var roomUserData: MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    val saveCallDuration: MutableLiveData<CallDurationResponse> = MutableLiveData()

    fun getRoomUserData(randomRoomData: RandomRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = bothTeamRepo.getRoomUserData(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomUserData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            //  ex.showAppropriateMsg()
        }
    }

    fun deleteUserRoomData(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = bothTeamRepo.deleteUsersDataFromRoom(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            // ex.showAppropriateMsg()
        }
    }

    fun saveCallDuration(callDuration: SaveCallDuration) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = bothTeamRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

}