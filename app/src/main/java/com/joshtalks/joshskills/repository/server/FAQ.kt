package com.joshtalks.joshskills.repository.server

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FAQ(
    @SerializedName("id")
    val id: Int,
    @SerializedName("question")
    val question: String,
    @SerializedName("answer")
    val answer: String,
    @SerializedName("faq_category")
    val categoryId: Int,
    @SerializedName("yes_count")
    val yesCount: Int,
    @SerializedName("no_count")
    val noCount: Int
) : Parcelable
