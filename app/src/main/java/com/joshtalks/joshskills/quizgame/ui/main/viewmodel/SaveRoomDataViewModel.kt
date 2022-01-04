package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.SaveRoomRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SaveRoomDataViewModel(var application11: Application) : BaseViewModel(application11) {

    val saveRoomRepo = SaveRoomRepo()
    var saveRoomDetailsData: MutableLiveData<Success> = MutableLiveData()
    var roomUserDataTemp: MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var clearRadius: MutableLiveData<Success> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    var fppData: MutableLiveData<Success> = MutableLiveData()
    var addToRoom: MutableLiveData<AddToRoomResponse> = MutableLiveData()
    var playAgainData: MutableLiveData<Success> = MutableLiveData()
    val saveCallDuration: MutableLiveData<CallDurationResponse> = MutableLiveData()

    fun saveRoomDetails(saveRoomDetails: SaveRoomDetails) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.saveRoomData(saveRoomDetails)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveRoomDetailsData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun getRoomUserDataTemp(randomRoomData: RandomRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.getRoomDataTemp(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomUserDataTemp.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun getClearRadius(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.clearRoomRadius(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        clearRadius.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun deleteUserRoomData(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.deleteUsersDataFromRoom(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    fun addFavouritePracticePartner(addFavouritePartner: AddFavouritePartner) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.addFavouritePartner(addFavouritePartner)
                    if (response?.isSuccessful == true && response.body() != null) {
                        fppData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun playAgainWithSamePlayer(playAgain: PlayAgain) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.playAgainData(playAgain)
                    if (response?.isSuccessful == true && response.body() != null) {
                        playAgainData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun saveCallDuration(callDuration: SaveCallDuration) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = saveRoomRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
        }
    }
}