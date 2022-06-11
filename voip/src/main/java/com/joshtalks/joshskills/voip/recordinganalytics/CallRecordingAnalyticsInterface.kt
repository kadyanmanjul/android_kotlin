package com.joshtalks.joshskills.voip.recordinganalytics

interface CallRecordingAnalyticsInterface {
    fun addAnalytics(agoraMentorId:String, agoraCallId:String? = null, localPath:String)
    suspend fun uploadAnalyticsToServer()
}

