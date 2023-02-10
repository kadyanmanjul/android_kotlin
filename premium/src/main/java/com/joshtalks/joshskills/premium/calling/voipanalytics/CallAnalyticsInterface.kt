package com.joshtalks.joshskills.premium.calling.voipanalytics

interface CallAnalyticsInterface {
    fun addAnalytics(event : EventName, agoraMentorId:String? = null, agoraCallId:String? = null, extra : String = "")
    suspend fun uploadAnalyticsToServer()
}

