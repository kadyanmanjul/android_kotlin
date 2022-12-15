package com.joshtalks.joshskills.common.repository.local.entity.groups

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.repository.local.model.Mentor
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

    fun getMentorId(): String? {
        messageId.substringAfterLast("_", Mentor.getInstance().getId()).let {
            return if (it.isBlank()) null
            else it
        }
    }

    private fun getColorHexCode(str: String): String {
        val colorArray = arrayOf(
            "#f83a7e", "#2213fa", "#d5857a",
            "#706d45", "#63805a", "#b812bc",
            "#ee431b", "#f56fbe", "#721fde",
            "#953f30", "#ed9207", "#8d8eb4",
            "#78bcb2", "#3c6c9b", "#6ce172",
            "#4dc7b6", "#fe5b00", "#846fd2",
            "#755812", "#3b9c42", "#c2d542",
            "#a22b2f", "#cc794a", "#c20748",
            "#7a4ff8", "#163d52"
        )
        return colorArray[Math.abs(str.hashCode()) % colorArray.size]
    }
}