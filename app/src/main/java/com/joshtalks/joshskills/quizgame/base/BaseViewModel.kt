package com.joshtalks.joshskills.quizgame.base

import android.app.Application
import android.os.Message
import androidx.lifecycle.AndroidViewModel

open class BaseViewModel(var application111: Application) : AndroidViewModel(application111) {
    protected var message = Message()

    protected var singleLiveEvent = EventLiveData
}