package com.joshtalks.joshskills.common.track

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class TrackActivity : AppCompatActivity(), LifecycleObserver {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    open fun onVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    try {
                        CourseUsageService.startTimeConversation(
                            this@TrackActivity,
                            it, this@TrackActivity.javaClass.simpleName
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    open fun onInVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    try {
                        CourseUsageService.endTimeConversation(this@TrackActivity, it)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    open fun getConversationId(): String? {
        return null
    }
}
