package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.Success
import com.joshtalks.joshskills.quizgame.ui.data.model.TeamDataDelete
import com.joshtalks.joshskills.quizgame.ui.data.model.UserDetails
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TeamMateFoundViewModel(
    var application111: Application,
    private val teamMateFoundRepo: TeamMateFoundRepo
) : AndroidViewModel(application111) {

    val userData: MutableLiveData<UserDetails> = MutableLiveData()

    val deleteData: MutableLiveData<Success> = MutableLiveData()

    fun getChannelData(mentorId: String) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
            if (UpdateReceiver.isNetworkAvailable(application111)) {
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
}