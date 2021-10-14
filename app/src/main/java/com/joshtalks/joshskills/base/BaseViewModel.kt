package com.joshtalks.joshskills.base

import android.app.Application
import android.os.Message
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import com.joshtalks.joshskills.base.SingleLiveEvent
import javax.inject.Inject

open class BaseViewModel @Inject constructor() : ViewModel(){
    @Inject
    protected lateinit var application: Application

    var isLoading = ObservableBoolean(false)

    protected var message = Message()

    @Inject
    protected lateinit var singleLiveEvent: SingleLiveEvent<Message>
}