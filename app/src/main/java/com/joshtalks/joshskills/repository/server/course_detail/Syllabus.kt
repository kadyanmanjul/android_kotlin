package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Syllabus(
    @SerializedName("icon_url")
    val iconUrl: String,

    @SerializedName("text")
    val text: String

) : Parcelable
