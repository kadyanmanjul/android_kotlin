package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SaveRoomDataViewProviderFactory(
    val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaveRoomDataViewModel::class.java)) {
            return SaveRoomDataViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}