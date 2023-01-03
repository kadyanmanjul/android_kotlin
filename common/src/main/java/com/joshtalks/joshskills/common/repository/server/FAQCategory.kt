package com.joshtalks.joshskills.common.repository.server

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class FAQCategory(
    @SerializedName("id")
    val id: Int,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("icon")
    val iconUrl: String,
    @SerializedName("sort_order")
    val sortOrder: Int
) : Parcelable
