package com.joshtalks.joshskills.premium.repository.server.chat_message

data class TUnlockClassMessage(
    val text: String
) : BaseChatMessage() {
    override val type: String = "UN"
}