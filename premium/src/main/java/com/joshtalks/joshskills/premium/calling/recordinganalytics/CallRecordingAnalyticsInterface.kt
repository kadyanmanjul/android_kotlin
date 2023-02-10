package com.joshtalks.joshskills.premium.calling.recordinganalytics

interface CallRecordingAnalyticsInterface {
    fun addAnalytics(agoraMentorId:String, agoraCallId:String? = null, localPath:String)
    suspend fun uploadAnalyticsToServer()
}

