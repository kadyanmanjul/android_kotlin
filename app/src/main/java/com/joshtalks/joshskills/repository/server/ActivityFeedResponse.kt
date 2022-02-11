package com.joshtalks.joshskills.repository.server

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
data class ActivityFeedList(
    @SerializedName("activity_feed_impression_id")
    val impressionId:String?,
)
