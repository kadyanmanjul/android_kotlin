package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SingleImageData(

    @SerializedName("imgUrl")
    val imgUrl: String

) : Parcelable
