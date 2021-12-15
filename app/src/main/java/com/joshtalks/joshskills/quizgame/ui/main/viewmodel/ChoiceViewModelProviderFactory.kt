package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.ChoiceRepo

class ChoiceViewModelProviderFactory(val app: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChoiceViewModel::class.java)) {
            return ChoiceViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}