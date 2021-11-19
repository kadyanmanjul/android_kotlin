package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.QuestionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class QuestionViewModel(application: Application,private val questionRepo: QuestionRepo) :AndroidViewModel(application){

   val questionData: MutableLiveData<QuestionResponse> = MutableLiveData()
   val selectOption : MutableLiveData<SelectOptionResponse> = MutableLiveData()
   val displayAnswerData : MutableLiveData<DisplayAnswerResponse> = MutableLiveData()
   var roomUserData:MutableLiveData<RoomUserData> = MutableLiveData()
   var roomUserDataTemp:MutableLiveData<RandomRoomResponse> = MutableLiveData()
   var clearRadius : MutableLiveData<Success> = MutableLiveData()


    fun getQuizQuestion(){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = questionRepo.getQuestion()
                if (response.isSuccessful && response.body()!=null){
                  //  words = arrayListOf(response.body())
                    questionData.postValue(response.body())
                }
            }
        }catch (ex:Throwable){

        }
    }

    fun getSelectOption(roomId:String, questionId:String, answerId : String, teamId:String){
        try {
            viewModelScope.launch (Dispatchers.IO){
                val response = questionRepo.getSelectAnswer(roomId,questionId,answerId,teamId)
                if (response.isSuccessful && response.body()!=null){
                    selectOption.postValue(response.body())
                }
            }
        }catch (ex:Throwable){

        }
    }

    fun getDisplayAnswerData(roomId:String, questionId:String){
        viewModelScope.launch (Dispatchers.IO){
            val response = questionRepo.getDisplayCorrectAnswer(roomId,questionId)
            if (response.isSuccessful && response.body()!=null){
                displayAnswerData.postValue(response.body())
            }
        }
    }

    fun getRoomUserDataTemp(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = questionRepo.getRoomDataTemp(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                roomUserDataTemp.postValue(response.body())
            }
        }
    }

    fun getClearRadius(randomRoomData: RandomRoomData){
        viewModelScope.launch (Dispatchers.IO){
            val response = questionRepo.clearRoomRadius(randomRoomData)
            if (response.isSuccessful && response.body()!=null){
                clearRadius.postValue(response.body())
            }
        }
    }
}
