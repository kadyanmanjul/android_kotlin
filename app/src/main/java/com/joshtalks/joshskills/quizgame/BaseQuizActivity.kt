package com.joshtalks.joshskills.quizgame

import android.os.Bundle
import android.widget.Toast
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.quizgame.base.EventLiveData

abstract class BaseQuizActivity : WebRtcMiddlewareActivity() {
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