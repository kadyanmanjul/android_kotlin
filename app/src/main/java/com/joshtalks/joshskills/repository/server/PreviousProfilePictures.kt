package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
data class PreviousProfilePictures(
    @SerializedName("label")
    val label: String = EMPTY,
    @SerializedName("pictures")
    val profilePictures: List<ProfilePicture> = listOf()
) : Parcelable

@Parcelize
data class ProfilePicture(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("photo_url")
    val photoUrl: String = EMPTY,
    @SerializedName("timestamp")
    val timestamp: Date? = null,
    @SerializedName("is_current_profile")
    val isCurrentProfile: Boolean = false
) : Parcelable
