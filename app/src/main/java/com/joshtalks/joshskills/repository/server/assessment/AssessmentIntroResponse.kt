package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentIntroResponse(

    @SerializedName("type")
    val type: AssessmentType,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String

) : Parcelable
