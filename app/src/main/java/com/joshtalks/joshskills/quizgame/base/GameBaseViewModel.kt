package com.joshtalks.joshskills.quizgame.base

import android.app.Application
import android.os.Message
import androidx.lifecycle.AndroidViewModel

open class GameBaseViewModel(var gameBaseviewModel: Application) : AndroidViewModel(gameBaseviewModel) {
    protected var message = Message()

    protected var singleLiveEvent = GameEventLiveData
}