package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo

class BothTeamViewProviderFactory(
    val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BothTeamViewModel::class.java)) {
            return BothTeamViewModel(app) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}