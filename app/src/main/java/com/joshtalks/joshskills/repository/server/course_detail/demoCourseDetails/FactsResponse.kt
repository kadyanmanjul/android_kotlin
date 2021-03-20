package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FactsResponse(
    @SerializedName("heading")
    val heading: String,
    @SerializedName("facts_list")
    val facts_list: List<Facts?>?
) : Parcelable

@Parcelize
data class Facts(
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("text")
    val text: String?
) : Parcelable
