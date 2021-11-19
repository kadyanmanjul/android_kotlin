package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchOpponentTeamViewModel(
    application: Application,
    private val searchOpponentRepo: SearchOpponentRepo) :
    AndroidViewModel(application) {

    val roomData: MutableLiveData<AddToRoomResponse> = MutableLiveData()
    var roomUserData:MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var deleteData : MutableLiveData<Success> = MutableLiveData()


    fun addToRoomData(teamId: ChannelName){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchOpponentRepo.addToRoomRepo(teamId)
                if (response.isSuccessful && response.body()!=null){
                    roomData.postValue(response.body())
                }
            }
        }catch (ex:Throwable){
            Timber.d(ex)
        }
    }

    fun getRoomUserData(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = searchOpponentRepo.getRoomUserData(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                roomUserData.postValue(response.body())
            }
        }
    }

    fun deleteUserAndTeamData(teamDataDelete: TeamDataDelete){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchOpponentRepo.deleteUserTeamData(teamDataDelete)
                if (response.isSuccessful && response.body()!=null){
                    deleteData.postValue(response.body())
                }
            }
        }catch (ex:Exception){

        }
    }


    fun deleteUserRoomData(randomRoomData: RandomRoomData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchOpponentRepo.deleteUsersDataFromRoom(randomRoomData)
                if (response.isSuccessful && response.body()!=null){
                    deleteData.postValue(response.body())
                }
            }
        }catch (ex:Exception){

        }
    }
}