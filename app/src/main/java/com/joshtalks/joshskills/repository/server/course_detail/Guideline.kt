package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Guideline(
    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("category")
    val category: String,

    @SerializedName("text")
    val text: List<String>

) : Parcelable
