package com.joshtalks.joshskills.ui.group.utils

import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.MessageItem

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