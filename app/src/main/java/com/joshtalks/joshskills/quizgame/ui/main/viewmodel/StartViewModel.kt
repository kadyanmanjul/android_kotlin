package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.quizgame.base.BaseViewModel
import com.joshtalks.joshskills.quizgame.ui.data.model.Success
import com.joshtalks.joshskills.quizgame.ui.data.repository.StartRepo
import com.joshtalks.joshskills.quizgame.util.OPEN_CHOICE_SCREEN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StartViewModel(var application11: Application) : BaseViewModel(application11) {
    var startRepo = StartRepo()
    var addData: MutableLiveData<Success> = MutableLiveData()
    val statusResponse: MutableLiveData<Success> = MutableLiveData()

    fun onItemClick(view: View) {
        message.what = OPEN_CHOICE_SCREEN
        singleLiveEvent.value = message
        view.visibility = View.GONE
    }

    fun addUserToDB() {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = startRepo.addUserInDb()
                    if (response?.isSuccessful == true && response.body() != null) {
                        addData.postValue(response.body())
                    }
                }
            }
        } catch (ex: Exception) { }
    }
    fun homeInactive() {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = startRepo.getHomeInactive()
                }
            }
        } catch (ex: Throwable) {
            showToast(ex.message?:"")
        }
    }

    fun statusChange() {
        try {
            if (Utils.isInternetAvailable()) {
                viewModelScope.launch(Dispatchers.IO) {
                    val response = startRepo.getStatus()
                    if (response?.isSuccessful == true && response.body() != null) {
                        statusResponse.postValue(response.body())
                    }
                }
            }
        } catch (ex: Throwable) {
            showToast(ex.message?:"")
        }
    }

}