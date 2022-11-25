package com.joshtalks.joshskills.common.repository.server.chat_message

class TVideoMessage(
    override var url: String, override var localPathUrl: String
) : BaseMediaMessage() {
    override val type: String = "VI"
}