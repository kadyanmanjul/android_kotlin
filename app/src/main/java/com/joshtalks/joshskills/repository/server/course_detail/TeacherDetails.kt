package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TeacherDetails(
    @SerializedName("name")
    val name: String,

    @SerializedName("designation")
    val designation: String,

    @SerializedName("dp_url")
    val dpUrl: String,

    @SerializedName("short_description")
    val shortDescription: String,

    @SerializedName("long_description")
    val longDescription: String,

    @SerializedName("bg_url")
    val bgUrl: String

) : Parcelable
