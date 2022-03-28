package com.joshtalks.joshskills.ui.activity_feed.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponse

data class ActivityFeedList(
    @SerializedName("activity_feed_impression_id")
    val impressionId:String?,
    @SerializedName("activites")
    val activityList:List<ActivityFeedResponse>?,
    @SerializedName("latest_timestamp")
    val timestamp:String?
)
