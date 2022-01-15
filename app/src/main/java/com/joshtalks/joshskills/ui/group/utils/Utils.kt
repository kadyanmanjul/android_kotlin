package com.joshtalks.joshskills.ui.group.utils

import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.MessageItem
import java.lang.Math.abs

fun getMemberCount(memberText: String): Int {
    var memberCount = 1
    if (memberText.isNotBlank()) {
        if (memberText.contains("practise partner calls"))
            return 2
        val num = memberText.split(" ")
        if (num.isNotEmpty())
            memberCount = num[0].toIntOrNull() ?: 1
    }
    return memberCount
}

fun MessageItem.getMessageType() = when (msgType) {
    MESSAGE ->
        if (mentorId == Mentor.getInstance().getId()) SENT_MESSAGE_LOCAL
        else RECEIVE_MESSAGE_LOCAL
    META_MESSAGE ->
        if (mentorId == Mentor.getInstance().getId()) SENT_META_MESSAGE_LOCAL
        else RECEIVE_META_MESSAGE_LOCAL
    UNREAD_MESSAGE -> UNREAD_MESSAGE
    else -> MESSAGE_ERROR
}

fun MessageItem.getLastMessage(sender: String, msgType: Int) =
    when {
        msgType == 1 ->
            if (mentorId == Mentor.getInstance().getId())
                msg.replace("$sender has", "You have")
            else msg
        mentorId == Mentor.getInstance().getId() -> "You: $msg"
        else -> "$sender: $msg"
    }

fun pushMetaMessage(msg: String, groupId: String) {
    val chatService: ChatService = PubNubService
    val message = MessageItem(
        msg = msg,
        msgType = META_MESSAGE,
        mentorId = Mentor.getInstance().getId()
    )
    chatService.sendMessage(groupId, message)
}

fun pushMetaRemoveMsg(msg: String, groupId: String, mentorId: String) {
    val chatService: ChatService = PubNubService
    val message = MessageItem(
        msg = msg,
        msgType = META_MESSAGE,
        mentorId = mentorId
    )
    chatService.sendMessage(groupId, message)
}

fun getColorHexCode(str: String): String {
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
    return colorArray[abs(str.hashCode()) % colorArray.size]
}