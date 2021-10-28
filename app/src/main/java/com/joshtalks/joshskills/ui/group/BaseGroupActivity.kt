package com.joshtalks.joshskills.ui.group

import android.os.Bundle
import android.widget.Toast
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity

abstract class BaseGroupActivity : WebRtcMiddlewareActivity() {
    protected var event = EventLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setIntentExtras()
        initViewBinding()
        onCreated()
        initViewState()
    }

    protected abstract fun setIntentExtras()
    protected abstract fun initViewBinding()
    protected abstract fun onCreated()
    protected abstract fun initViewState()

    protected fun showToast(msg : String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}