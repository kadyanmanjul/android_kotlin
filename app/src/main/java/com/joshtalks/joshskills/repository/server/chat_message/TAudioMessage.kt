package com.joshtalks.joshskills.repository.server.chat_message

class TAudioMessage(
    override var url: String, override var localPathUrl: String

) : BaseMediaMessage() {
    override val type: String = "AU"
}