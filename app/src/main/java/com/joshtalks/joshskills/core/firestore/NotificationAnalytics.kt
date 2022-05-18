package com.joshtalks.joshskills.core.firestore

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber

private const val TAG = "CallAnalytics"
class NotificationAnalytics {
    private val notificationDao by lazy {
        AppObjectController.appDatabase.notificationEventDao()
    }
    private val mutex = Mutex()

    fun addAnalytics(notificationId:String,event: String, channel: String?) {
        Log.d(
            "Manjul",
            "addAnalytics() called with: notificationId = $notificationId, event = $event, channel = $channel"
        )
        val notificationEvent = NotificationEvent(
            action = event,
            time_stamp = System.currentTimeMillis(),
            platform = channel,
            id = notificationId
        )
        pushAnalytics(notificationEvent)
    }

    suspend fun getNotification(notificationId:String): List<NotificationEvent>? {
            return notificationDao.getNotificationEvent(notificationId)
    }

    private fun pushAnalytics(event: NotificationEvent) {
        if (event.id.isNullOrEmpty()) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationDao.insertNotificationEvent(event)
            } catch (e: Exception) {
                if (e is CancellationException)
                    throw e
                e.printStackTrace()
            }
        }
    }

    private fun <T> T.serializeToMap(): Map<String, Any> {
        return convert()
    }

    private inline fun <I, reified O> I.convert(): O {
        val gson = Gson()
        val json = gson.toJson(this)
        return gson.fromJson(json, object : TypeToken<O>() {}.type)
    }
}

