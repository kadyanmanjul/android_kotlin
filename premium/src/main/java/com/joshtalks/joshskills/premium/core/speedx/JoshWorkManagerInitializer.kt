package com.joshtalks.joshskills.premium.core.speedx

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import com.facebook.FacebookSdk
import com.facebook.internal.FacebookInitProvider
import com.freshchat.consumer.sdk.f.c
import java.lang.Exception


class JoshWorkManagerInitializer : Initializer<WorkManager> {
    override fun create(context: Context): WorkManager {
        val configuration = Configuration.Builder().build()
        WorkManager.initialize(context, configuration)
        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}