package com.joshtalks.joshskills.voip.voipanalytics

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.data.local.VoipDatabase
import com.joshtalks.joshskills.voip.voipanalytics.data.local.VoipAnalyticsEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import timber.log.Timber


private const val TAG = "CallAnalytics"
object CallAnalytics : CallAnalyticsInterface{

    private val database by lazy {
        Utils.context?.let { VoipDatabase.getDatabase(it.applicationContext) }
    }

    private val mutex = Mutex()

    override fun addAnalytics(event: EventName, agoraMentorId: String?, agoraCallId: String?) {
        val callEvent = CallEvents(event = event, timestamp = System.currentTimeMillis().toString(), agoraCallId = agoraCallId, agoraMentorId = agoraMentorId)
//        pushAnalytics(callEvent)
    }

    private fun pushAnalyticsToServer(eventHashMap: Map<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                val analyticsList = database?.voipAnalyticsDao()?.getAnalytics()
                if (analyticsList != null) {
                    for (analytics in analyticsList) {
                        try {
                            val response = callAnalyticsApi(eventHashMap)
                            if (response.isSuccessful) {
                                database?.voipAnalyticsDao()?.deleteAnalytics(analytics.id)
                            }
                        } catch (e: Exception) {
                            Timber.tag("VOIP_ANALYTICS").e("Error Occurred")
                            e.printStackTrace()
                            if(e is CancellationException)
                                throw e
                        }
                    }
                }
            }
        }
    }

    private fun pushAnalytics(event: CallEvents) {
        if(event.agoraCallId?.isEmpty() == true || event.agoraMentorId?.isEmpty() == true) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try{
                val analyticsData = VoipAnalyticsEntity(
                    event = event.event.eventName,
                    agoraCallId = event.agoraCallId?:"",
                    agoraMentorUid = event.agoraMentorId?:"",
                    timeStamp = event.timestamp.toString()
                )
                database?.voipAnalyticsDao()?.saveAnalytics(analyticsData)
                val eventHashMap = event.serializeToMap()
                pushAnalyticsToServer(eventHashMap)
            }
            catch (e : Exception){
                if(e is CancellationException)
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
