package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BothTeamViewModel(
    application: Application,
    private val bothTeamRepo: BothTeamRepo) :
    AndroidViewModel(application) {

    var roomUserData:MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var deleteData : MutableLiveData<Success> = MutableLiveData()

    fun getRoomUserData(randomRoomData: RandomRoomData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = bothTeamRepo.getRoomUserData(randomRoomData)
                if (response.isSuccessful && response.body()!=null){
                    roomUserData.postValue(response.body())
                }
            }
        }catch (ex:Exception){
            ex.showAppropriateMsg()
        }
    }

    fun deleteUserRoomData(randomRoomData: RandomRoomData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = bothTeamRepo.deleteUsersDataFromRoom(randomRoomData)
                if (response.isSuccessful && response.body()!=null){
                    deleteData.postValue(response.body())
                }
            }
        }catch (ex:Exception){
            ex.showAppropriateMsg()
        }
    }
}