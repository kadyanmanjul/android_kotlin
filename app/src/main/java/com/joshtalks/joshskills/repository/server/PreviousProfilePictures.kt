package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize


@Parcelize
data class PreviousProfilePictures(
    @SerializedName("label")
    val label: String = EMPTY,
    @SerializedName("pictures")
    val pictures: List<Picture> = listOf()
) : Parcelable
