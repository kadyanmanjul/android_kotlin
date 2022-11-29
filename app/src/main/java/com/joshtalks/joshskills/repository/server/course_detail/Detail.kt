package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Detail(
    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("title")
    val title: String

) : Parcelable
