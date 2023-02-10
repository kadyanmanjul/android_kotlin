package com.joshtalks.joshskills.premium.calling.recordinganalytics

data class CallRecordingEvents(
    val timestamp: String,
    val agoraCallId: String? = null,
    val agoraMentorId: String? = null,
    val localPath: String
)