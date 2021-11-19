package com.joshtalks.joshskills.quizgame.ui.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.quizgame.ui.data.repository.SaveRoomRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.SearchOpponentRepo
import com.joshtalks.joshskills.quizgame.ui.data.repository.TeamMateFoundRepo

class SaveRoomDataViewProviderFactory(
    val app: Application,
    val saveRoomRepo: SaveRoomRepo
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaveRoomDataViewModel::class.java)) {
            return SaveRoomDataViewModel(app, saveRoomRepo) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }

}