package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import java.util.*

data class ChatMessageReceiver(
    @SerializedName("conversation")
    val conversationId: String,
    @SerializedName("created")
    val created: Date,
    @SerializedName("id")
    val id: String,
    @SerializedName("is_seen")
    val isSeen: Boolean,
    @SerializedName("question")
    val question: Any,
    @SerializedName("sender")
    val sender: String,
    @SerializedName("text")
    val text: String?,
    @SerializedName("type")
    var type: BASE_MESSAGE_TYPE?,
    @SerializedName("url")
    val url: String?
)