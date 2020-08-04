package com.joshtalks.joshskills.repository.server.chat_message

data class TUnlockClassMessage(
    val text: String
) : BaseChatMessage() {
    override val type: String = "UN"
}
