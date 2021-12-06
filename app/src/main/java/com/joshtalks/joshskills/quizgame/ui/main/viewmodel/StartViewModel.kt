package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.quizgame.ui.data.model.*
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.StartRepo
import com.joshtalks.joshskills.quizgame.util.UpdateReceiver
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartViewModel(
    var application111: Application,
    private val startRepo: StartRepo
) :
    AndroidViewModel(application111) {

    var addData: MutableLiveData<Success> = MutableLiveData()

    fun addUserToDB(addUserDb: AddUserDb) {
        try {
            if (UpdateReceiver.isNetworkAvailable(application111)) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = startRepo.addUser(addUserDb)
                    if (response?.isSuccessful == true && response.body() != null) {
                        addData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) {
          //  ex.showAppropriateMsg()
        }
    }
}