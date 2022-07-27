package com.joshtalks.badebhaiya.core

import android.app.Application
import androidx.lifecycle.*
import androidx.multidex.MultiDexApplication
import com.joshtalks.badebhaiya.utils.Utils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class JoshApplication: MultiDexApplication(), LifecycleObserver {

    companion object {
        @Volatile
        public var isAppVisible = false
    }

    override fun onCreate() {
        super.onCreate()
        AppObjectController.init(this)
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleEventObserver)
        Utils.initUtils(this)
    }

    private val lifecycleEventObserver = LifecycleEventObserver { source, event ->
        if (event == Lifecycle.Event.ON_START ) {
            isAppVisible = true
        }
        else if ( event == Lifecycle.Event.ON_STOP ) {
            isAppVisible = false
        }
    }
}