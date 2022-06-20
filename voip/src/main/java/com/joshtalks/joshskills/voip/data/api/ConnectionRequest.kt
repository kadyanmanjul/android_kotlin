package com.joshtalks.joshskills.voip.data.api

import com.google.gson.annotations.SerializedName

data class ConnectionRequest(

    @field:SerializedName("course_id")
    val courseId: Int? = null,

    @field:SerializedName("mentor_id")
    val mentorId: String? = null,

    @field:SerializedName("topic_id")
    val topicId: Int? = null,

    @field:SerializedName("old_call_id")
    val oldCallId: Int? = null
)

data class CallActionRequest(

    @field:SerializedName("response")
    val response: String? = null,

    @field:SerializedName("mentor_id")
    val mentorId: String? = null,

    @field:SerializedName("call_id")
    val callId: Int? = null
)

data class CallDisconnectRequest(
    @field:SerializedName("response")
    val response: String? = null,

    @field:SerializedName("mentor_id")
    val mentorId: String? = null,

    @field:SerializedName("channel_name")
    val channelName: String? = null,

	@field:SerializedName("duration")
	val duration: Long? = null
)

data class CallRecordingRequest(
    @field:SerializedName("agora_call")
    val agoraCallId: String? = null,

    @field:SerializedName("agora_mentor")
    val agoraMentorId: String? = null,

    @field:SerializedName("recording_url")
    val recording_url: String,

    @field:SerializedName("duration")
    val duration: Int? = null
)