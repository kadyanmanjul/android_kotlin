package com.joshtalks.joshskills.common.repository.server.chat_message

abstract class BaseChatMessage {
    var conversation: String = ""
    abstract val type: String

}