package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchRandomRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchRandomUserViewModel(
    var application111: Application,
    private val searchRandomRepo: SearchRandomRepo
) :
    AndroidViewModel(application111) {

    var searchRandomData: MutableLiveData<SearchRandomResponse> = MutableLiveData()
    //var statusResponse: MutableLiveData<Success> = MutableLiveData()
    var roomRandomData: MutableLiveData<RoomData> = MutableLiveData()
    var randomRoomUser: MutableLiveData<RandomRoomDataResponse> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    var clearRadius: MutableLiveData<Success> = MutableLiveData()
    fun getSearchRandomUserData(userId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchRandomRepo.getSearchRandomData(userId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        searchRandomData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun createRoomRandom(roomRandom: RoomRandom) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchRandomRepo.createRandomUserRoom(roomRandom)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomRandomData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun getRandomUserDataByRoom(randomRoomData: RandomRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchRandomRepo.getRandomUserData(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        randomRoomUser.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
           // showToast(ex.message ?: "")
        }
    }

    fun deleteUserRadiusData(deleteUserData: DeleteUserData) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchRandomRepo.deleteUserData(deleteUserData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun getClearRadius(randomRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = searchRandomRepo.clearRoomRadius(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        clearRadius.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }
}