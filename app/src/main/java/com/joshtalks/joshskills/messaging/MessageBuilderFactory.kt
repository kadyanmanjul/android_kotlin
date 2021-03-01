package com.joshtalks.joshskills.messaging

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.chat_message.*
import com.joshtalks.joshskills.util.RandomString
import java.util.*

object MessageBuilderFactory {

    fun getMessage(
        cMessageType: BASE_MESSAGE_TYPE,
        message: BaseChatMessage,
    ): ChatModel {
        when (cMessageType) {
            BASE_MESSAGE_TYPE.TX -> {
                return getTextChatModel(message)
            }
            BASE_MESSAGE_TYPE.VI -> {
                return getVideoChatModel(message)

            }
            BASE_MESSAGE_TYPE.IM -> {
                return getImageChatModel(message)

            }
            BASE_MESSAGE_TYPE.AU -> {
                return getAudioChatModel(message)
            }
            BASE_MESSAGE_TYPE.UNLOCK -> {
                return getUnlockChatModel(message)
            }//add new
            else -> return ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        }

    }

    private fun getUnlockChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        model.text = (message as TUnlockClassMessage).text
        model.type = BASE_MESSAGE_TYPE.UNLOCK
        model.created = Date(System.currentTimeMillis())
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.chatLocalId = RandomString().nextString()
        model.isSync = true
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        return model
    }

    private fun getTextChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        model.text = (message as TChatMessage).text
        model.type = BASE_MESSAGE_TYPE.TX
        model.created = Date(System.currentTimeMillis())
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.chatLocalId = RandomString().nextString()
        model.isSync = true
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        return model
    }

    private fun getAudioChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        model.type = BASE_MESSAGE_TYPE.AU

        model.url = (message as TAudioMessage).url
        model.created = Date(System.currentTimeMillis())
        model.downloadStatus = DOWNLOAD_STATUS.UPLOADING
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        model.downloadedLocalPath = model.url
        model.isSync = true
        model.chatLocalId = RandomString().nextString()
        model.url?.let {
            model.duration =
                Utils.getDurationOfMedia(AppObjectController.joshApplication, it)?.toInt() ?: 0
        }
        return model
    }

    private fun getImageChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        model.url = (message as TImageMessage).url
        model.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.created = Date(System.currentTimeMillis())
        model.downloadedLocalPath = message.localPathUrl
        model.isSync = true
        model.type = BASE_MESSAGE_TYPE.IM
        model.chatLocalId = RandomString().nextString()
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        return model
    }


    private fun getVideoChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel(BASE_MESSAGE_TYPE.NEW_CLASS)
        model.url = (message as TVideoMessage).url
        model.created = Date(System.currentTimeMillis())
        model.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        model.downloadedLocalPath = model.url
        model.type = BASE_MESSAGE_TYPE.VI
        model.isSync = true
        model.chatLocalId = RandomString().nextString()
        model.url?.let {
            model.duration =
                Utils.getDurationOfMedia(AppObjectController.joshApplication, it)?.toInt() ?: 0
        }
        return model
    }


}