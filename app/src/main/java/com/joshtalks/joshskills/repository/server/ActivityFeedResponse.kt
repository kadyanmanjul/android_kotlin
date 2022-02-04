package com.joshtalks.joshskills.repository.server

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
data class ActivityFeedList(
    @SerializedName("activity_feed_impression_id")
    val impressionId:String?,
//    @SerializedName("activites")
//    var feedList: List<ActivityFeedResponse>
)
//data class ActivityFeedResponse(
//    @SerializedName("id")
//    var id: Int?=null,
//    @SerializedName("name")
//    var name: String?=null,
//    @SerializedName("text")
//    var text: String?=null,
//    @SerializedName("date")
//    var date: String?=null,
//    @SerializedName("photo_url")
//    var photoUrl: String?=null
//)

