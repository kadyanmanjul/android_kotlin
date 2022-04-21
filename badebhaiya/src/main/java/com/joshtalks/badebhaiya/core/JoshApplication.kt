package com.joshtalks.badebhaiya.core

import android.app.Application
import androidx.lifecycle.*
import androidx.multidex.MultiDexApplication
import timber.log.Timber

class JoshApplication : MultiDexApplication(), LifecycleObserver {

    companion object {
        @Volatile
        public var isAppVisible = false
    }

    override fun onCreate() {
        super.onCreate()
        AppObjectController.init(this)
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleEventObserver)
    }

    private val lifecycleEventObserver = LifecycleEventObserver { source, event ->
        when (event) {
            Lifecycle.Event.ON_START -> isAppVisible = true
            Lifecycle.Event.ON_STOP -> isAppVisible = false
            Lifecycle.Event.ON_DESTROY -> endTheRoom()
            else -> {}
        }
    }

    private fun endTheRoom() {
//        TODO: End The Room if there's any room
    }
}