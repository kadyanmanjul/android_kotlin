package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Picture(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("photo_url")
    val photoUrl: String = EMPTY,
    @SerializedName("timestamp")
    val timestamp: String = EMPTY,
    @SerializedName("is_current_profile")
    val isCurrentProfile: Boolean = false
) : Parcelable
