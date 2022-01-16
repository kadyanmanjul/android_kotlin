package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TeamMateViewProviderFactory(
    val app: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamMateFoundViewModel::class.java)) {
            return TeamMateFoundViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}