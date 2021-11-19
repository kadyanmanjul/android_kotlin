package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SaveRoomRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveRoomDataViewModel(
    application: Application,
    private val saveRoomRepo: SaveRoomRepo) :
    AndroidViewModel(application) {

    var saveRoomDetailsData:MutableLiveData<Success> = MutableLiveData()
    var roomUserDataTemp:MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var clearRadius : MutableLiveData<Success> = MutableLiveData()

    fun saveRoomDetails(saveRoomDetails: SaveRoomDetails){
        viewModelScope.launch (Dispatchers.IO){
            val response = saveRoomRepo.saveRoomData(saveRoomDetails)
            if (response.isSuccessful && response.body()!=null){
                saveRoomDetailsData.postValue(response.body())
            }
        }
    }

    fun getRoomUserDataTemp(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = saveRoomRepo.getRoomDataTemp(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                roomUserDataTemp.postValue(response.body())
            }
        }
    }

    fun getClearRadius(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = saveRoomRepo.clearRoomRadius(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                clearRadius.postValue(response.body())
            }
        }
    }
}