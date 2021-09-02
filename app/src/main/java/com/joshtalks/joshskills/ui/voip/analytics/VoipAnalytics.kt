package com.joshtalks.joshskills.ui.voip.analytics

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsEntity
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_CALL_ID_API_KEY
import com.joshtalks.joshskills.ui.voip.analytics.data.network.VOIP_ANALYTICS_DISCONNECT_API_KEY
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
        Event.ON_HOLD.value,
        Event.CALL_RESTORED.value
    )
    private val DISCONNECT_CALL_ANALYTICS = setOf(
        Event.DISCONNECT.AGORA_CALL_RESPONSE_FAILURE.value,
        Event.DISCONNECT.FORCE_DISCONNECT_NOTIFICATION_FAILURE.value,
        Event.DISCONNECT.CALL_DISCONNECT_NOTIFICATION_FAILURE.value,
        Event.DISCONNECT.USER_DISCONNECTED_FAILURE.value,
        Event.DISCONNECT.RECONNECTING_FAILURE.value,
        Event.DISCONNECT.NO_USER_FOUND_FAILURE.value,
        Event.DISCONNECT.BACK_BUTTON_FAILURE.value
    )

    enum class Event(override val value: String) : VoipEvent {
        CALL_CONNECT_SCREEN_VISUAL("call_started_at"),
        RECEIVED_INCOMING_NOTIFICATION("delivered_at"),
        INCOMING_SCREEN_VISUAL("shown_at"),
        CALL_DECLINED("declined_at"),
        CALL_ACCEPT("picked_at"),
        SPEAKING("mic_started_at"),
        LISTENING("speaker_started_at"),
        USER_DID_NOT_PICKUP_CALL("ignored_at"),
        RECONNECTING("RECONNECTING"),
        CALL_RESTORED("CALL_RESTORED"),
        RESUME("RESUME"),
        ON_HOLD("ONHOLD");

        enum class DISCONNECT(override val value : String) : VoipEvent {
            AGORA_CALL_RESPONSE_FAILURE("AGORA_CALL_RESPONSE"),
            FORCE_DISCONNECT_NOTIFICATION_FAILURE("FORCE_DISCONNECT_NOTIFICATION"),
            CALL_DISCONNECT_NOTIFICATION_FAILURE("CALL_DISCONNECT_NOTIFICATION"),
            USER_DISCONNECTED_FAILURE("USER_DISCONNECTED"),
            RECONNECTING_FAILURE("RECONNECTING"),
            NO_USER_FOUND_FAILURE("NO_USER_FOUND"),
            BACK_BUTTON_FAILURE("BACK_BUTTON"),
            AGORA_LIBRARY_FAILURE("AGORA_LIBRARY"),
            LOCATION_PERMISSION_FAILURE("LOCATION_PERMISSION"),
            AGORA_USER_OFFLINE_FAILURE("AGORA_USER_OFFLINE")
        }
    }

    fun push(event: VoipEvent, agoraCallId: String, agoraMentorUid: String, timeStamp: String) {
        if(agoraCallId.isEmpty() || agoraMentorUid.isEmpty())
            return
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
        } else if(analyticsData.event in DISCONNECT_CALL_ANALYTICS)
            request[VOIP_ANALYTICS_DISCONNECT_API_KEY] = analyticsData.event
        else
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

interface VoipEvent {
    val value : String
}

/*
fun main() {
    print("${DateUtils.getCurrentTimeStamp()}")
}*/
