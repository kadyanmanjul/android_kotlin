package com.joshtalks.joshskills.explore.course_details.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Guidelines(
    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("guidelines")
    val guidelines: List<Guideline>

) : Parcelable
