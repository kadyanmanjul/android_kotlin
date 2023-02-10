package com.joshtalks.joshskills.premium.calling.voipanalytics

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.premium.calling.Utils
import com.joshtalks.joshskills.premium.calling.data.api.VoipNetwork
import com.joshtalks.joshskills.premium.calling.data.local.VoipDatabase
import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics.serializeToMap
import com.joshtalks.joshskills.premium.calling.voipanalytics.data.local.VoipAnalyticsEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import timber.log.Timber


private const val TAG = "CallAnalytics"

object CallAnalytics : CallAnalyticsInterface {

    private val database by lazy {
        Utils.context?.let { VoipDatabase.getDatabase(it.applicationContext) }
    }

    override fun addAnalytics(
        event: EventName,
        agoraMentorId: String?,
        agoraCallId: String?,
        extra: String
    ) {
        val callEvent = CallEvents(
            event = event,
            timestamp = Utils.getCurrentTimeStamp(),
            agoraCallId = agoraCallId,
            agoraMentorId = agoraMentorId,
            extra = extra
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val eventHashMap = VoipAnalyticsEntity(
                    type = callEvent.event.eventName,
                    timestamp = callEvent.timestamp,
                    agora_call = callEvent.agoraCallId.toString(),
                    extra = callEvent.extra,
                    agora_mentor = callEvent.agoraMentorId.toString()
                ).serializeToMap()
                val response = callAnalyticsApi(eventHashMap)
                if(response.isSuccessful.not())
                    pushAnalytics(callEvent)
            } catch (e : Exception) {
                e.printStackTrace()
                pushAnalytics(callEvent)
            }
        }
    }

    override suspend fun uploadAnalyticsToServer() {
        pushAnalyticsToServer()
    }

    private suspend fun pushAnalyticsToServer() {
        val analyticsList = database?.voipAnalyticsDao()?.getAnalytics()
        if (analyticsList != null) {
            for (analytics in analyticsList) {
                try {
                    val eventHashMap = analytics.serializeToMap()
                    val response = callAnalyticsApi(eventHashMap)
                    if (response.isSuccessful) {
                        database?.voipAnalyticsDao()?.deleteAnalytics(analytics.id)
                    }
                } catch (e: Exception) {
                    Timber.tag("VOIP_ANALYTICS").e("Error Occurred")
                    e.printStackTrace()
                    if (e is CancellationException)
                        throw e
                }
            }
        }
    }

    private fun pushAnalytics(event: CallEvents) {
        if (event.agoraCallId?.isEmpty() == true || event.agoraMentorId?.isEmpty() == true) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val analyticsData = VoipAnalyticsEntity(
                    type = event.event.eventName,
                    agora_call = event.agoraCallId ?: "",
                    agora_mentor = event.agoraMentorId ?: "",
                    timestamp = event.timestamp.toString(),
                    extra = event.extra
                )
                database?.voipAnalyticsDao()?.saveAnalytics(analyticsData)
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

    @JvmSuppressWildcards
    private suspend fun callAnalyticsApi(request: Map<String, Any?>): Response<Unit> {
        return VoipNetwork.getVoipAnalyticsApi().agoraMidCallDetails(request)
    }

}
