package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LongDescription(
    @SerializedName("description")
    val description: String,

    @SerializedName("title")
    val title: String

) : Parcelable
