package com.joshtalks.joshskills.ui.activity_feed.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class ActivityFeedResponseFirebase(
    @SerializedName("id")
    var id: Int?=null,
    @SerializedName("name")
    var name: String?=null,
    @SerializedName("text")
    var text: String?=null,
    @SerializedName("date")
    var date: Timestamp?=null,
    @SerializedName("photo_url")
    var photoUrl: String?=null
)