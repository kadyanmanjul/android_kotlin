package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.base.GameBaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.network.RetrofitInstance
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TeamMateFoundViewModelGame(var teamMateVm: Application, ) : GameBaseViewModel(teamMateVm) {
    val teamMateFoundRepo = TeamMateFoundRepo(RetrofitInstance.getRetrofitInstance())

    val userData: MutableLiveData<UserDetails> = MutableLiveData()

    val deleteData: MutableLiveData<Success> = MutableLiveData()

    val saveCallDuration: MutableLiveData<CallDurationResponse> = MutableLiveData()

    fun getChannelData(mentorId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = teamMateFoundRepo.getUserDetails(mentorId)
                    if (response?.isSuccessful == true && response.body() != null) {
                        userData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            Timber.d(ex)
        }
    }

    fun deleteUserRadiusData(teamDataDelete: TeamDataDelete) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = teamMateFoundRepo.deleteUserData(teamDataDelete)
                    if (response?.isSuccessful == true && response.body() != null) {
                        deleteData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }

    fun saveCallDuration(callDuration: SaveCallDuration) {
        try {
            if (UpdateReceiver.isNetworkAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = teamMateFoundRepo.saveDurationOfCall(callDuration)
                    if (response?.isSuccessful == true && response.body() != null) {
                        saveCallDuration.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {

        }
    }
}