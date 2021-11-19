package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.FavouriteRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo

class TeamMateViewProviderFactory(
    val app: Application,
    val appRepository: TeamMateFoundRepo
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamMateFoundViewModel::class.java)) {
            return TeamMateFoundViewModel(app, appRepository) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}