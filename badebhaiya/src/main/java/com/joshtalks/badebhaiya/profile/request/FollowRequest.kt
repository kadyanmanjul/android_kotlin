package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName

class FollowRequest(
    @SerializedName("followee")
    val followeeId: String,
    @SerializedName("follower")
    val followerId: String
)
