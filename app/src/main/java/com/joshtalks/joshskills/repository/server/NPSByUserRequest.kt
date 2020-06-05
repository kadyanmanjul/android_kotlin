package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
data class NPSByUserRequest(

    @SerializedName("instance")
    val instanceId: String?,
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("event_name")
    val eventName: String?,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("text")
    val text: String?,
    @SerializedName("course")
    val courseId: String?
)