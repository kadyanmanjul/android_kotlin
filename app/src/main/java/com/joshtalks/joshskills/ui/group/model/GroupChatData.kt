package com.joshtalks.joshskills.ui.group.model

import com.joshtalks.joshskills.ui.group.constants.MessageType

interface GroupChatData {
    fun getUniqueId(): String
    fun getTitle(): String
    fun getMessage(): String
    fun getMessageTime(): String
    fun getType(): MessageType
}