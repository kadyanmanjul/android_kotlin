package com.joshtalks.joshskills.repository.server.introduction


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Screen(
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("video_url")
    val videoUrl: String?
) :Parcelable