package com.joshtalks.joshskills.base

import android.os.Message
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel() {
    protected var message = Message()

    protected var singleLiveEvent = EventLiveData
}