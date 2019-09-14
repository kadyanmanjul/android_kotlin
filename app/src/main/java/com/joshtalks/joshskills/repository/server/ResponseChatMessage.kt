package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.ChatModel

data class ResponseChatMessage (
    @SerializedName("results") var chatModelList: List<ChatModel>,

    @SerializedName("count") var count: Int,

    @SerializedName("next") var next: String?,

    @SerializedName("previous") var previous: String?

)