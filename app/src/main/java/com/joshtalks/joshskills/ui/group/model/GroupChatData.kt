package com.joshtalks.joshskills.ui.group.model

interface GroupChatData {
    fun getUniqueId(): String
    fun getTitle(): String
    fun getMessage(): String
    fun getMessageTime(): String
    fun getType(): MessageType
}