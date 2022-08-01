package com.joshtalks.joshskills.base

import android.os.Message
import androidx.lifecycle.ViewModel
import javax.inject.Inject

open class BaseViewModel @Inject constructor(): ViewModel() {
    protected var message = Message()

    protected var singleLiveEvent = EventLiveData
}