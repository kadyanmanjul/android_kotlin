package com.joshtalks.joshskills.track

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
    fun onVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    CourseUsageService.startTimeConversation(
                        this@TrackActivity,
                        it,
                        javaClass.simpleName
                    )
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onInVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    CourseUsageService.endTimeConversation(this@TrackActivity, it)
                }
            }
        }
    }

    open fun getConversationId(): String? {
        return null
    }
}
