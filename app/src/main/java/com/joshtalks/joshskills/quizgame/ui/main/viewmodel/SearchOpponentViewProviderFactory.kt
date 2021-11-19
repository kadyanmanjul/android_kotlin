package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo

class SearchOpponentViewProviderFactory(
    val app: Application,
    val appRepository: SearchOpponentRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchOpponentTeamViewModel::class.java)) {
            return SearchOpponentTeamViewModel(app, appRepository) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}