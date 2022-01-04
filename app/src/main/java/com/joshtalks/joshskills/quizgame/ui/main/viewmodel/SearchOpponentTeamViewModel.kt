package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchOpponentTeamViewModel(var application11: Application) : BaseViewModel(application11) {
    var searchOpponentRepo = SearchOpponentRepo()
    val roomData: MutableLiveData<AddToRoomResponse> = MutableLiveData()
    var roomUserData: MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    var saveDuration: MutableLiveData<Success> = MutableLiveData()
    val saveCallDuration: MutableLiveData<CallDurationResponse> = MutableLiveData()

    fun addToRoomData(teamId: ChannelName) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchOpponentRepo.addToRoomRepo(teamId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun getRoomUserData(randomRoomData: RandomRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchOpponentRepo.getRoomUserData(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomUserData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun deleteUserAndTeamData(teamDataDelete: TeamDataDelete) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchOpponentRepo.deleteUserTeamData(teamDataDelete)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            showToast(ex.message ?: "")
        }
    }

    fun deleteUserRoomData(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response =
                        searchOpponentRepo.deleteUsersDataFromRoom(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
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
                    val response = searchOpponentRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
        }
    }
}