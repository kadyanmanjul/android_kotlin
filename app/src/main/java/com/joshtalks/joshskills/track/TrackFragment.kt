package com.joshtalks.joshskills.track

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class TrackFragment : Fragment(), LifecycleObserver {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    try {
                        CourseUsageService.startTimeConversation(
                            requireContext(),
                            it, this@TrackFragment.javaClass.simpleName
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onInVisibleScreen() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (getConversationId().isNullOrEmpty().not()) {
                getConversationId()?.let {
                    try {
                        CourseUsageService.endTimeConversation(requireContext(), it)
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
