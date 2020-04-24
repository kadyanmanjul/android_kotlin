package com.joshtalks.joshskills.repository.server

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ResponseChatMessage(
    @SerializedName("results") var chatModelList: List<ChatModel>,

    @SerializedName("count") var count: Int,

    @SerializedName("next") var next: String?,

    @SerializedName("previous") var previous: String?

) : Parcelable