package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchOpponentTeamViewModel(
    var application111: Application,
    private val searchOpponentRepo: SearchOpponentRepo
) :
    AndroidViewModel(application111) {

    val roomData: MutableLiveData<AddToRoomResponse> = MutableLiveData()
    var roomUserData: MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    var saveDuration: MutableLiveData<Success> = MutableLiveData()

    fun addToRoomData(teamId: ChannelName) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchOpponentRepo.deleteUserTeamData(teamDataDelete)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun deleteUserRoomData(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
}