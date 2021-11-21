package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchRandomRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SearchRandomUserViewModel(
    application: Application,
    private val searchRandomRepo: SearchRandomRepo) :
    AndroidViewModel(application) {

    var searchRandomData:MutableLiveData<SearchRandomResponse> = MutableLiveData()
    var statusResponse : MutableLiveData<Success> = MutableLiveData()
    var roomRandomData : MutableLiveData<RoomData> = MutableLiveData()
    var randomRoomUser : MutableLiveData<RandomRoomDataResponse> = MutableLiveData()
    var deleteData : MutableLiveData<Success> = MutableLiveData()
    var clearRadius  :MutableLiveData<Success> = MutableLiveData()
    fun getSearchRandomUserData(userId:String){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchRandomRepo.getSearchRandomData(userId)
                if (response.isSuccessful && response.body()!=null){
                    searchRandomData.postValue(response.body())
                }
            }
        }catch (ex:Exception){

        }
    }

    fun statusChange(userIdMentor: String?,status:String?){
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val response = searchRandomRepo.getStatus(userIdMentor,status)
                if (response.isSuccessful && response.body()!=null){
                    statusResponse.postValue(response.body())
                }
            }
        }catch (ex:Throwable){
            Timber.d(ex)
        }
    }

    fun createRoomRandom(roomRandom: RoomRandom){
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val response = searchRandomRepo.createRandomUserRoom(roomRandom)
                if (response.isSuccessful && response.body()!=null){
                    roomRandomData.postValue(response.body())
                }
            }
        }catch (ex:Throwable){
            Timber.d(ex)
        }
    }

    fun getRandomUserDataByRoom(randomRoomData: RandomRoomData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchRandomRepo.getRandomUserData(randomRoomData)
                if (response.isSuccessful && response.body()!=null){
                    randomRoomUser.postValue(response.body())
                }
            }
        }catch (ex:Exception){
            showToast(ex.message?:"")
            Log.d("error_resp", "getRandomUserDataByRoom: "+ex.message)
        }
    }

    fun deleteUserRadiusData(deleteUserData: DeleteUserData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchRandomRepo.deleteUserData(deleteUserData)
                if (response.isSuccessful && response.body()!=null){
                    deleteData.postValue(response.body())
                }
            }
        }catch (ex:Exception){

        }
    }

    fun getClearRadius(randomRoomData: RandomRoomData){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = searchRandomRepo.clearRoomRadius(randomRoomData)
                if (response.isSuccessful && response.body()!=null){
                    clearRadius.postValue(response.body())
                }
            }
        }catch (ex:Exception){

        }
    }

}