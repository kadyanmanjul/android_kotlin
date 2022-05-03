package com.joshtalks.joshskills.voip.voipanalytics

interface CallAnalyticsInterface {
    fun addAnalytics(event : EventName, agoraMentorId:String? = null, agoraCallId:String? = null)


}

