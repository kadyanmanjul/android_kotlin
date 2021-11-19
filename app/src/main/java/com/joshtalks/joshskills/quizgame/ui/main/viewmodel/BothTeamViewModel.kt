package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BothTeamViewModel(
    application: Application,
    private val bothTeamRepo: BothTeamRepo) :
    AndroidViewModel(application) {

    var roomUserData:MutableLiveData<RandomRoomResponse> = MutableLiveData()

    fun getRoomUserData(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = bothTeamRepo.getRoomUserData(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                roomUserData.postValue(response.body())
            }
        }
    }
}