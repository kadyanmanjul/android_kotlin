package com.joshtalks.joshskills.explore.course_details.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(

    @SerializedName("rating")
    val rating: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("dp_url")
    val dpUrl: String?,

    @SerializedName("username")
    val username: String,

    @SerializedName("user_location")
    val userLocaton: String

) : Parcelable
