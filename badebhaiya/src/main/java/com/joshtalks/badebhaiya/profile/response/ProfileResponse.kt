package com.joshtalks.badebhaiya.profile.response

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("bio") val bioText: String,
    @SerializedName("created_source") val created_source: String,
    @SerializedName("date_of_birth") val date_of_birth: Any,
    @SerializedName("email") val email: Any,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("followers_count") val followersCount: Int,
    @SerializedName("gender") val gender: Any,
    @SerializedName("hometown") val hometown: Any,
    @SerializedName("id") val userId: String,
    @SerializedName("is_skills_user") val is_skills_user: Boolean,
    @SerializedName("is_speaker") val isSpeaker: Boolean,
    @SerializedName("last_active_on") val last_active_on: Any,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("photo_url") val profilePicUrl: String?,
    @SerializedName("social_id") val social_id: Any,
    @SerializedName("user_type") val user_type: String,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("user_room_data") val SpeakerRooms: List<Any>
)
