package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

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

    @SerializedName("user_locaton")
    val userLocaton: String

) : Parcelable
