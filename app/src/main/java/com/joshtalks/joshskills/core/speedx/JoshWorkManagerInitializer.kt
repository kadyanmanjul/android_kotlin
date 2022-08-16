package com.joshtalks.joshskills.core.speedx

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager

class JoshWorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        Log.e("SukeshTest", "Initializing WM")
        val configuration = Configuration.Builder().build()
        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return mutableListOf(JoshAppInitializer::class.java)
    }
}