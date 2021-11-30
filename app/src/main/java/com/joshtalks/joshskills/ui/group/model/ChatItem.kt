package com.joshtalks.joshskills.ui.group.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.core.Utils
import java.util.Date

@Entity(tableName = "group_chat_db")
data class ChatItem(

    @PrimaryKey(autoGenerate = true)
    val messageId: Long = 0,

    val sender: String?,

    var message: String,

    val msgTime: Long,

    val groupId: String,

    val msgType: Int
) {

    fun getMessageTime() = Utils.getMessageTimeInHours(Date(msgTime / 10000))
}