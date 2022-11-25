package com.joshtalks.joshskills.common.repository.server.chat_message


abstract class BaseMediaMessage : BaseChatMessage() {
    abstract var url: String
    abstract var localPathUrl: String


}

