package com.joshtalks.joshskills.repository.server

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import java.util.Date
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LeaderboardMentor(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("award_url")
    val award_url: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("points")
    val points: Int,
    @SerializedName("ranking")
    var ranking: Int,
    @SerializedName("is_online")
    var isOnline: Boolean = false,
    @SerializedName("is_senior_student")
    val isSeniorStudent: Boolean = false,
    @SerializedName("is_course_bought")
    val isCourseBought: Boolean = false,
    @SerializedName("expire_date")
    val expiryDate: Date? = null,
) : Parcelable
