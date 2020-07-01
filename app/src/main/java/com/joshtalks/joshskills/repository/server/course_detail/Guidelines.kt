package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Guidelines(
    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("guidelines")
    val guidelines: List<Guideline>

) : Parcelable
