package com.joshtalks.badebhaiya.feed.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.profile.request.FollowRequest

data class Users(
    val user_id:String,
    val short_name:String,
    @SerializedName("full_name")
    val full_name:String,
    @SerializedName("photo_url")
    val profilePic:String,
    val bio:String,
    val is_speaker_followed : Boolean
)