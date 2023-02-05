package com.joshtalks.joshskills.premium.repository.server.chat_message

abstract class BaseChatMessage {
    var conversation: String = ""
    abstract val type: String

}