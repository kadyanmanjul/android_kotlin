package com.joshtalks.joshskills.repository.server.chat_message

abstract class BaseChatMessage {
    var conversation: String = ""
    abstract val type: String

}