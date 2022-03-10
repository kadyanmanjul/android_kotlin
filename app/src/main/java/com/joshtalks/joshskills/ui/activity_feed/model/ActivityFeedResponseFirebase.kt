package com.joshtalks.joshskills.ui.activity_feed.model

import com.google.gson.annotations.SerializedName

data class ActivityFeedResponseFirebase(
    @SerializedName("date")
    var date: String? = null,
    @SerializedName("event_id")
    var eventId: String? = null,
    @SerializedName("media_url")
    var mediaUrl: String? = null,
    @SerializedName("mentor_id")
    var mentorId: String? = null,
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("photo_url")
    var photoUrl: String? = null,
    @SerializedName("text")
    var text: String? = null
)

