package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SearchOpponentViewProviderFactory(
    val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchOpponentTeamViewModelGame::class.java)) {
            return SearchOpponentTeamViewModelGame(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}