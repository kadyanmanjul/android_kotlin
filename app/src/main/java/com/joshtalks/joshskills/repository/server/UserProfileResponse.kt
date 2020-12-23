package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class UserProfileResponse(
    @SerializedName("age")
    val age: Int?,
    @SerializedName("createdAt")
    val createdAt: Int?,
    @SerializedName("date_of_birth")
    val dateOfBirth: Int?,
    @SerializedName("joined_on")
    val joinedOn: String?,
    @SerializedName("lastActiveAt")
    val lastActiveAt: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("color_code")
    val colorCode: String?,
    @SerializedName("points")
    val points: Int?,
    @SerializedName("minutes_spoken")
    val minutesSpoken: Int?,
    @SerializedName("role")
    val role: String?,
    @SerializedName("status")
    val status: String?,
    @SerializedName("streak")
    val streak: Int?,
    @SerializedName("uid")
    val uid: String?,
    @SerializedName("is_points_active")
    val isPointsActive: Boolean? = false,
    @SerializedName("award_category_list")
    var awardCategory: List<AwardCategory>?,
    @SerializedName("certificates")
    val certificates: List<Certificate>?,
    @SerializedName("group_info")
    val groupInfo: List<GroupInfo>?
)

data class GroupInfo(
    @SerializedName("group_id")
    val groupId: String?,
    @SerializedName("group_name")
    val groupName: String?,
    @SerializedName("total_audio_messages")
    val totalAudioMessages: Int?,
    @SerializedName("total_duration")
    val totalDuration: Int?,
    @SerializedName("total_messages")
    val totalMessages: Int?
)

data class Certificate(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("certificate_text")
    val certificateText: String?,
    @SerializedName("date_text")
    val dateText: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("certificate_description")
    val certificateDescription: String?,
    @SerializedName("is_achieved")
    val is_achieved: Boolean=false,
    @SerializedName("is_seen")
    val isSeen: Boolean?
)
@Parcelize
data class AwardCategory(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("awards")
    var awards: List<Award>?
) :Parcelable

@Parcelize
data class Award(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("award_text")
    val awardText: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("date_text")
    val dateText: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("award_description")
    val awardDescription: String?,
    @SerializedName("is_achieved")
    val is_achieved: Boolean=false,
    @SerializedName("is_seen")
    val isSeen: Boolean?
) : Parcelable
