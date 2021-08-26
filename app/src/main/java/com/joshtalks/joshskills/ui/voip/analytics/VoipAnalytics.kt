package com.joshtalks.joshskills.ui.voip.analytics

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

object VoipAnalytics {
    private val database by lazy {
        AppDatabase.getDatabase(AppObjectController.joshApplication)
    }

    private val mutex = Mutex()

    enum class Event(val value: String) {
        OUTGOING_SCREEN_VISUAL(""),
        RECEIVED_INCOMING_NOTIFICATION(""),
        INCOMING_SCREEN_VISUAL(""),
        CALL_DECLINED(""),
        CALL_ACCEPT(""),
        USER_DID_NOT_PICKUP_CALL(""),
        RECONNECTING("")
    }

    fun push(event: Event) {
        CoroutineScope(Dispatchers.IO).launch {
            val analyticsData = VoipAnalyticsEntity(event.value)
            database?.voipAnalyticsDao()?.saveAnalytics(analyticsData)
        }
    }

    fun pushToServer() {
        // Get Data From Database and push
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                // TODO: Get Data from Database
                val analyticsList = database?.voipAnalyticsDao()?.getAnalytics() ?: mutableListOf()
                //TODO: Sent data to network
                for (analytics in analyticsList) {
                    try {
                        val response =
                            AppObjectController.voipAnalyticsService.pushVoipAnalyticsToServer()
                        database?.voipAnalyticsDao()?.deleteAnalytics(1)
                    } catch (e: Exception) {
                        Timber.tag("VOIP_ANALYTICS").e("Error Occurred")
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}