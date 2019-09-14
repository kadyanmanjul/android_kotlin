package com.joshtalks.joshskills.repository.server.chat_message

data class TImageMessage(
    override var url: String, override var localPathUrl: String

) : BaseMediaMessage() {
    override val type: String = "IM"
}