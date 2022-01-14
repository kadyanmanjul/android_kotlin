package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.QuestionRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionViewModel(var application11: Application) :
    BaseViewModel(application11) {

    val questionRepo = QuestionRepo()
    val questionData: MutableLiveData<QuestionResponse> = MutableLiveData()
    val selectOption: MutableLiveData<SelectOptionResponse> = MutableLiveData()
    val displayAnswerData: MutableLiveData<DisplayAnswerResponse> = MutableLiveData()
    var roomUserDataTemp: MutableLiveData<RandomRoomResponse> = MutableLiveData()
    var clearRadius: MutableLiveData<Success> = MutableLiveData()
    var deleteData: MutableLiveData<Success> = MutableLiveData()
    val saveCallDuration :MutableLiveData<CallDurationResponse> = MutableLiveData()


    fun getQuizQuestion(questionRequest: QuestionRequest) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.getQuestion(questionRequest)
                    if (response?.isSuccessful == true && response.body() != null) {
                        questionData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }

    fun getSelectOption(roomId: String, questionId: String, answerId: String, teamId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response =
                        questionRepo.getSelectAnswer(roomId, questionId, answerId, teamId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        selectOption.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }

    fun getDisplayAnswerData(roomId: String, questionId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.getDisplayCorrectAnswer(roomId, questionId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        displayAnswerData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    fun getRoomUserDataTemp(randomRoomData: RandomRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.getRoomDataTemp(randomRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        roomUserDataTemp.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    fun getClearRadius(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.clearRoomRadius(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        clearRadius.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    fun deleteUserRoomData(saveCallDurationRoomData: SaveCallDurationRoomData) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.deleteUsersDataFromRoom(saveCallDurationRoomData)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    fun saveCallDuration(callDuration: SaveCallDuration) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = questionRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }
}
