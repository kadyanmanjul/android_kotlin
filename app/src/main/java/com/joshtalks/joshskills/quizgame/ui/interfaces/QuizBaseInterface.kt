package com.joshtalks.joshskills.quizgame.ui.interfaces

import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite

interface QuizBaseInterface {
    fun onClickForGetToken(favourite: Favourite?)
}