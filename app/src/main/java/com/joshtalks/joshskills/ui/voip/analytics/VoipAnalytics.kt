package com.joshtalks.joshskills.ui.voip.analytics

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsEntity
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_CALL_ID_API_KEY
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_MENTOR_UID_API_KEY
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_TIMESTAMP_API_KEY
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_TYPE_API_KEY
import com.joshtalks.joshskills.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object VoipAnalytics {
    private val database by lazy {
        AppDatabase.getDatabase(AppObjectController.joshApplication)
    }

    private val mutex = Mutex()
    private val MID_CALL_ANALYTICS = setOf(
        Event.RECONNECTING.value,
        Event.RESUME.value,
        Event.ON_HOLD.value
    )

    enum class Event(val value: String) {
        CALL_CONNECT_SCREEN_VISUAL("call_started_at"),
        RECEIVED_INCOMING_NOTIFICATION("delivered_at"),
        INCOMING_SCREEN_VISUAL("shown_at"),
        CALL_DECLINED("declined_at"),
        CALL_ACCEPT("picked_at"),
        USER_DID_NOT_PICKUP_CALL("ignored_at"),
        RECONNECTING("RECONNECTING"),
        RESUME("RESUME"),
        ON_HOLD("ONHOLD")
    }

    fun push(event: Event, agoraCallId: String, agoraMentorUid: String, timeStamp: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val analyticsData = VoipAnalyticsEntity(
                event.value,
                agoraCallId = agoraCallId,
                agoraMentorUid = agoraMentorUid,
                timeStamp = timeStamp
            )
            database?.voipAnalyticsDao()?.saveAnalytics(analyticsData)
            pushToServer()
        }
    }

    fun pushIncomingCallAnalytics(actionData: String) {
        val obj = JSONObject(actionData)
        val agoraCallId = try {
            obj.getString("agoraCallId")
        } catch (e: JSONException) {
            ""
        }
        val agoraMentorUid = obj.getString("uid")
        val channelName = obj.getString("channel_name")
        val callerUid = obj.getString("caller_uid")


        CurrentCallDetails.set(
            callieUid = agoraMentorUid,
            callId = agoraCallId,
            channelName = channelName,
            callerUid = callerUid
        )
        push(
            Event.RECEIVED_INCOMING_NOTIFICATION,
            agoraCallId = agoraCallId,
            agoraMentorUid = agoraMentorUid,
            timeStamp = DateUtils.getCurrentTimeStamp()
        )
    }

    fun pushToServer() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val analyticsList = database?.voipAnalyticsDao()?.getAnalytics() ?: mutableListOf()
                for (analytics in analyticsList) {
                    try {
                        val request = getApiRequest(analytics)
                        val response = callAnalyticsApi(request, analytics.event)
                        if (response.isSuccessful)
                            database?.voipAnalyticsDao()?.deleteAnalytics(analytics.id)
                    } catch (e: Exception) {
                        Timber.tag("VOIP_ANALYTICS").e("Error Occurred")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getApiRequest(analyticsData: VoipAnalyticsEntity): Map<String, Any?> {
        val request = mutableMapOf<String, String>().apply {
            this[VOIP_ANALYTICS_CALL_ID_API_KEY] = analyticsData.agoraCallId
            this[VOIP_ANALYTICS_MENTOR_UID_API_KEY] = analyticsData.agoraMentorUid
        }
        if (analyticsData.event in MID_CALL_ANALYTICS) {
            request[VOIP_ANALYTICS_TIMESTAMP_API_KEY] = analyticsData.timeStamp
            request[VOIP_ANALYTICS_TYPE_API_KEY] = analyticsData.event
        } else
            request[analyticsData.event] = analyticsData.timeStamp
        return request
    }

    @JvmSuppressWildcards
    private suspend fun callAnalyticsApi(request: Map<String, Any?>, eventType: String) =
        if (eventType in MID_CALL_ANALYTICS)
            AppObjectController.voipAnalyticsService.agoraMidCallDetails(request)
        else
            AppObjectController.voipAnalyticsService.agoraCallDetails(request)
}

/*
fun main() {
    print("${DateUtils.getCurrentTimeStamp()}")
}*/
