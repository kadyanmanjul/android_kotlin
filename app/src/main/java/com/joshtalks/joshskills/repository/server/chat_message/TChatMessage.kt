package com.joshtalks.joshskills.repository.server.chat_message

data class TChatMessage(
    val text: String
) : BaseChatMessage() {
    override val type: String = "TX"
}