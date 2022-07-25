package com.joshtalks.joshskills.core.speedx

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager

class JoshWorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}