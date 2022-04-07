package com.joshtalks.badebhaiya.profile.response

import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem

data class ProfileResponse(
    @SerializedName("bio") val bioText: String?,
    @SerializedName("created_source") val created_source: String,
    @SerializedName("date_of_birth") val date_of_birth: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("gender") val gender: String?,
    @SerializedName("hometown") val hometown: String?,
    @SerializedName("id") val userId: String,
    @SerializedName("is_skills_user") val is_skills_user: Boolean,
    @SerializedName("is_speaker") val isSpeaker: Boolean,
    @SerializedName("last_active_on") val last_active_on: Any,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("photo_url") val profilePicUrl: String?,
    @SerializedName("social_id") val social_id: String?,
    @SerializedName("user_type") val user_type: String,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("is_speaker_followed") val isSpeakerFollowed: Boolean,
    @SerializedName("live_room") val liveRoomList: List<RoomListResponseItem>?,
    @SerializedName("scheduled_room") val scheduledRoomList: List<RoomListResponseItem>?
)
