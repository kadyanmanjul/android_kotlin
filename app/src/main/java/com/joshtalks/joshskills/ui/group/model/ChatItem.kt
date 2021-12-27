package com.joshtalks.joshskills.ui.group.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.ui.group.utils.getColorHexCode
import java.util.Date

@Entity(tableName = "group_chat_db")
data class ChatItem(

    @PrimaryKey
    @NonNull
    val messageId: String,

    val sender: String?,

    var message: String,

    val msgTime: Long,

    val groupId: String,

    val msgType: Int
) {

    fun getMessageTime(): String {
        val time = Utils.getMessageTimeInHours(Date(msgTime / 10000))
        return if (time[0] == '0') time.substring(1) else time
    }

    fun getColorFromId() = getColorHexCode(messageId.substring(messageId.lastIndexOf("_") + 1))
}