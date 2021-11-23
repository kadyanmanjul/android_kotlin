package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.core.Utils
import java.util.Date

@Entity(tableName = "group_chat_db")
data class ChatItem(

    @PrimaryKey(autoGenerate = true)
    val message_id: Long,

    val sender: String?,

    val message: String,

    val message_time: Long,

    val groupId: String
) {

    constructor(sender: String?, message: String, message_time: Long, groupId: String) : this(
        0,
        sender,
        message,
        message_time,
        groupId
    )

    fun getMessageTime() = Utils.getMessageTimeInHours(Date(message_time / 10000))

    fun getType(): MessageType {
        return MessageType.RECEIVED_MESSAGE
    }
}