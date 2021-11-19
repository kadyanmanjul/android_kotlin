package com.joshtalks.joshskills.quizgame.ui.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserDetails(
    @SerializedName("user_uid") var userUid:String?,
    @SerializedName("name") var name:String?,
    @SerializedName("image_url") var imageUrl:String?
) :Parcelable