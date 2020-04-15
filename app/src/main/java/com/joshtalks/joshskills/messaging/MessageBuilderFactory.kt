package com.joshtalks.joshskills.messaging

import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.chat_message.*
import com.joshtalks.joshskills.ui.view_holders.*
import com.joshtalks.joshskills.util.RandomString
import java.lang.ref.WeakReference
import java.util.*

object MessageBuilderFactory {

    fun getMessage(
        activityRef: WeakReference<FragmentActivity>,
        cMessageType: BASE_MESSAGE_TYPE,
        message: BaseChatMessage
    ): BaseChatViewHolder {
        when (cMessageType) {
            BASE_MESSAGE_TYPE.TX -> {
                return TextViewHolder(activityRef, getTextChatModel(message))

            }
            BASE_MESSAGE_TYPE.VI -> {
                return VideoViewHolder(activityRef, getVideoChatModel(message))

            }
            BASE_MESSAGE_TYPE.IM -> {
                return ImageViewHolder(activityRef, getImageChatModel(message))

            }
            BASE_MESSAGE_TYPE.AU -> {
                return AudioPlayerViewHolder(activityRef, getAudioChatModel(message))
            }
            else -> return TextViewHolder(activityRef, ChatModel())
        }

    }

    private fun getTextChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel()
        model.text = (message as TChatMessage).text
        model.type = BASE_MESSAGE_TYPE.TX
        model.created = Date(System.currentTimeMillis())
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.chatLocalId = RandomString().nextString()
        model.isSync = false
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        return model
    }

    private fun getAudioChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel()
        model.type = BASE_MESSAGE_TYPE.AU

        model.url = (message as TAudioMessage).url
        model.created = Date(System.currentTimeMillis())
        model.downloadStatus = DOWNLOAD_STATUS.UPLOADING
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        model.downloadedLocalPath = model.url
        model.isSync = false

        model.chatLocalId = RandomString().nextString()
        model.url?.let {
            model.mediaDuration = Utils.getDurationOfMedia(AppObjectController.joshApplication, it)
        }
        return model
    }

    private fun getImageChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel()
        model.url = (message as TImageMessage).url
        model.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.created = Date(System.currentTimeMillis())
        model.downloadedLocalPath = message.localPathUrl
        model.isSync = false
        model.type = BASE_MESSAGE_TYPE.IM
        model.chatLocalId = RandomString().nextString()
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        return model
    }


    private fun getVideoChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel()
        model.url = (message as TVideoMessage).url
        model.created = Date(System.currentTimeMillis())
        model.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
        model.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT
        model.sender = Sender(Mentor.getInstance().getId(), User(), "")
        model.downloadedLocalPath = model.url
        model.type = BASE_MESSAGE_TYPE.VI
        model.isSync = false
        model.chatLocalId = RandomString().nextString()
        model.url?.let {
            model.mediaDuration = Utils.getDurationOfMedia(AppObjectController.joshApplication, it)
        }
        return model
    }


}