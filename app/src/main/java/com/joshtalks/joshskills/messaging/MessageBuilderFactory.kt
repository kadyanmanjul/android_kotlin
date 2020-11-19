package com.joshtalks.joshskills.messaging

import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.Sender
import com.joshtalks.joshskills.repository.local.entity.User
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TAudioMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.repository.server.chat_message.TUnlockClassMessage
import com.joshtalks.joshskills.repository.server.chat_message.TVideoMessage
import com.joshtalks.joshskills.ui.view_holders.AudioPlayerViewHolder
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.joshtalks.joshskills.ui.view_holders.ImageViewHolder
import com.joshtalks.joshskills.ui.view_holders.TextViewHolder
import com.joshtalks.joshskills.ui.view_holders.UnlockNextClassViewHolder
import com.joshtalks.joshskills.ui.view_holders.VideoViewHolder
import com.joshtalks.joshskills.util.RandomString
import java.lang.ref.WeakReference
import java.util.*

object MessageBuilderFactory {

    fun getMessage(
        activityRef: WeakReference<FragmentActivity>,
        cMessageType: BASE_MESSAGE_TYPE,
        message: BaseChatMessage,
        prevMessage:ChatModel?
    ): BaseChatViewHolder {
        when (cMessageType) {
            BASE_MESSAGE_TYPE.TX -> {
                return TextViewHolder(activityRef, getTextChatModel(message),prevMessage)
            }
            BASE_MESSAGE_TYPE.VI -> {
                return VideoViewHolder(activityRef, getVideoChatModel(message),prevMessage)

            }
            BASE_MESSAGE_TYPE.IM -> {
                return ImageViewHolder(activityRef, getImageChatModel(message),prevMessage)

            }
            BASE_MESSAGE_TYPE.AU -> {
                return AudioPlayerViewHolder(activityRef, getAudioChatModel(message),prevMessage)
            }
            BASE_MESSAGE_TYPE.UNLOCK -> {
                return UnlockNextClassViewHolder(activityRef, getUnlockChatModel(message),prevMessage)
            }//add new
            else -> return TextViewHolder(activityRef, ChatModel(),prevMessage)
        }

    }

    private fun getUnlockChatModel(message: BaseChatMessage): ChatModel {
        val model = ChatModel()
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