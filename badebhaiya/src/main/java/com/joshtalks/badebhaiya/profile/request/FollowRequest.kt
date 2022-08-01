package com.joshtalks.badebhaiya.profile.request

import com.google.gson.annotations.SerializedName

class FollowRequest(
    @SerializedName("followee")
    val followeeId: String,
    @SerializedName("follower")
    val followerId: String,
    @SerializedName("is_from_speakers_to_follow_page")
    val isFromBBPage: Boolean,
    @SerializedName("is_from_deeplink")
    val isFromDeepLink:Boolean,
    @SerializedName("from_page")
    val from:String
)
