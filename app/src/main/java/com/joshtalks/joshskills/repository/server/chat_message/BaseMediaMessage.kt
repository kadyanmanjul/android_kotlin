package com.joshtalks.joshskills.repository.server.chat_message


abstract class BaseMediaMessage : BaseChatMessage() {
    abstract var url:String
    abstract var localPathUrl: String


}

