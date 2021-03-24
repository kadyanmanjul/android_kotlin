package com.joshtalks.joshskills.track

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

abstract class TrackActivity : AppCompatActivity(), LifecycleObserver {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onVisibleScreen() {
        if (getConversationId().isNullOrEmpty().not()) {
            getConversationId()?.let {
                CourseUsageService.startTimeConversation(this, it, javaClass.simpleName)
            }
            return
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onInVisibleScreen() {
        if (getConversationId().isNullOrEmpty().not()) {
            getConversationId()?.let {
                CourseUsageService.endTimeConversation(this, it)
            }
            return
        }
    }

    open fun getConversationId(): String? {
        return null
    }
}
