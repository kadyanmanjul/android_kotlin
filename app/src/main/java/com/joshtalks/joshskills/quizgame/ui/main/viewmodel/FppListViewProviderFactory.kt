package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FppListViewProviderFactory(
    val app: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavouriteViewModelGame::class.java)) {
            return FavouriteViewModelGame(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}