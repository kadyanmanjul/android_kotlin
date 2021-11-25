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

    val msgTime: Long,

    val groupId: String,

    val msgType: Int
) {

    constructor(sender: String?, message: String, msgTime: Long, groupId: String, msgType: Int) : this(
        0,
        sender,
        message,
        msgTime,
        groupId,
        msgType
    )

    fun getMessageTime() = Utils.getMessageTimeInHours(Date(msgTime / 10000))
}