package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS

data class MessageStatusRequest(
    @SerializedName("id")
    val id: String
) {
    @SerializedName("status")
    val status: String = MESSAGE_STATUS.SEEN_BY_SERVER.name
}