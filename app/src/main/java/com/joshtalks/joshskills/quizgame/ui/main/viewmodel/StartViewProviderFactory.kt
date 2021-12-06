package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.BothTeamRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.StartRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo

class StartViewProviderFactory(
    val app: Application,
    val appRepository: StartRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartViewModel::class.java)) {
            return StartViewModel(app, appRepository) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}