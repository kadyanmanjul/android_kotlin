package com.joshtalks.joshskills.common.repository.server.chat_message

data class TUnlockClassMessage(
    val text: String
) : BaseChatMessage() {
    override val type: String = "UN"
}
