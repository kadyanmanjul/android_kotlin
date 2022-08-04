package com.joshtalks.badebhaiya.feed.model

import androidx.collection.ArraySet
import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import java.util.ArrayList
//sdf
data class Users(
    @SerializedName("uuid")
    val user_id:String = "",
    @SerializedName("short_name")
    val short_name:String = "",
    @SerializedName("full_name")
    val full_name:String = "",
    @SerializedName("photo_url")
    val profilePic:String?,
    @SerializedName("bio")
    val bio:String?  = "",
    @SerializedName("is_speaker_followed")
    var is_speaker_followed : Boolean
)

data class Fans(
    @SerializedName("user_id")
    val user_id:String="",
    @SerializedName("short_name")
    val shortName:String="",
    @SerializedName("full_name")
    val fullName:String="",
    @SerializedName("photo_url")
    val profilePic:String=""
    )

data class Waiting(
    @SerializedName("user_id")
    val user_id:String = "",
    @SerializedName("short_name")
    val short_name:String = "",
    @SerializedName("full_name")
    val full_name:String = "",
    @SerializedName("photo_url")
    val profilePic:String?,
)

data class WaitingList(
    @SerializedName("users")
    val users:List<Waiting>
)