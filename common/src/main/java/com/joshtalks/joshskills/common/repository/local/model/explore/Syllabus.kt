package com.joshtalks.joshskills.common.repository.local.model.explore

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Syllabus(
    @SerializedName("icon_url")
    val iconUrl: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("sort_order")
    val sortOrder: Int

) : Parcelable
