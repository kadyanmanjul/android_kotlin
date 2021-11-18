package com.joshtalks.joshskills.ui.group.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.android.parcel.Parcelize

@Parcelize
data class GroupChatResponse(
    @field:SerializedName("chats")
    val chats: List<GroupChat?>? = null
) : Parcelable

@Parcelize
data class GroupChat(

    //TODO: Complete the serialized names as per API

    @field:SerializedName("")
    val sender_id: String? = null,

    @field:SerializedName("")
    val message_text: String,

    @field:SerializedName("")
    val message_time: String
) : Parcelable, GroupChatData {
    override fun getTitle(): String {
        return sender_id ?: ""
    }

    override fun getMessage(): String {
        return message_text
    }

    override fun messageTime(): String {
        return message_time
    }

    override fun getType(): MessageType {
        return when {
            sender_id.isNullOrEmpty() -> MessageType.META_DATA
            sender_id == Mentor.getInstance().getId() -> MessageType.SENT_MESSAGE
            else -> MessageType.RECEIVED_MESSAGE
        }
    }
}