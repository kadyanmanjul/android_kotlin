package com.joshtalks.joshskills.voip.recordinganalytics

data class CallRecordingEvents(
    val timestamp: String,
    val agoraCallId: String? = null,
    val agoraMentorId: String? = null,
    val localPath: String
)