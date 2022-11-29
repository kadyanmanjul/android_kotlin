package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Syllabus(
    @SerializedName("icon_url")
    val iconUrl: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("sort_order")
    val sortOrder: Int

) : Parcelable
