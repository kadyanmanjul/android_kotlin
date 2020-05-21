package com.joshtalks.joshskills.repository.service

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val HEADER_KEY = "createdmilisecond"

object NetworkRequestHelper {

    fun getUpdatedChat(
        conversation_id: String,
        queryMap: Map<String, String> = emptyMap()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = AppObjectController.chatNetworkService.getUnReceivedMessageAsync(
                    conversation_id,
                    queryMap
                )
                if (resp.chatModelList.isNullOrEmpty()) {
                    RxBus2.publish(MessageCompleteEventBus(false))
                } else {
                    PrefManager.put(
                        conversation_id.trim(),
                        resp.chatModelList.last().messageTimeInMilliSeconds
                    )
                    RxBus2.publish(MessageCompleteEventBus(true))
                }


                for (chatModel in resp.chatModelList) {
                    val chatObj =
                        AppObjectController.appDatabase.chatDao()
                            .getNullableChatObject(chatModel.chatId)
                    if (chatObj == null) {
                        chatModel.downloadStatus = DOWNLOAD_STATUS.NOT_START
                        AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
                    } else {
                        chatObj.chatId = chatModel.chatId
                        chatObj.url = chatModel.url
                        chatObj.isSeen = true
                        chatObj.conversationId = chatModel.conversationId
                        chatObj.created = chatModel.created
                        chatObj.messageDeliverStatus = chatModel.messageDeliverStatus
                        chatObj.type = chatModel.type
                        AppObjectController.appDatabase.chatDao().updateChatMessage(chatObj)
                    }
                    chatModel.question?.let { question ->
                        question.chatId = chatModel.chatId
                        AppObjectController.appDatabase.chatDao().insertChatQuestion(question)
                        question.audioList?.let {
                            it.listIterator().forEach { audioType ->
                                audioType.questionId = question.questionId
                                DownloadUtils.downloadAudioFile(it)
                            }

                            AppObjectController.appDatabase.chatDao().insertAudioMessageList(it)
                        }

                        question.imageList?.let {
                            it.listIterator().forEach { imageType ->
                                imageType.questionId = question.questionId
                            }
                            AppObjectController.appDatabase.chatDao().insertImageTypeMessageList(it)
                        }

                        question.optionsList?.let {
                            it.listIterator().forEach { optionType ->
                                optionType.questionId = question.questionId
                            }
                            AppObjectController.appDatabase.chatDao()
                                .insertOptionTypeMessageList(it)

                        }

                        question.pdfList?.let {
                            it.listIterator().forEach { pdfType ->
                                pdfType.questionId = question.questionId
                            }
                            AppObjectController.appDatabase.chatDao().insertPdfMessageList(it)

                        }
                        question.videoList?.let {
                            it.listIterator().forEach { videoType ->
                                videoType.questionId = question.questionId
                                videoType.downloadStatus = DOWNLOAD_STATUS.NOT_START
                            }
                            AppObjectController.appDatabase.chatDao().insertVideoMessageList(it)
                        }
                    }
                }
                RxBus2.publish(DBInsertion("Chat"))

                resp.next?.let {
                    val uri = Uri.parse(it)
                    val args = uri.queryParameterNames
                    val arguments = mutableMapOf<String, String>()
                    PrefManager.getLastSyncTime(conversation_id).let { keys ->
                        /*for (name in args) {
                            uri.getQueryParameter(name)?.run {
                                arguments[name] = this
                            }
                        }*/
                        arguments[keys.first] = keys.second
                    }
                    getUpdatedChat(conversation_id, queryMap = arguments)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    fun updateChat(
        chatMessageReceiver: ChatMessageReceiver,
        refreshViewLiveData: MutableLiveData<ChatModel>? = null,
        messageObject: BaseChatMessage, currentChatModel: ChatModel?
    ) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {

            val chatModel = ChatModel(
                chatId = chatMessageReceiver.id,
                conversationId = chatMessageReceiver.conversationId,
                text = chatMessageReceiver.text,
                type = chatMessageReceiver.type,
                sender = Sender(
                    id = chatMessageReceiver.sender,
                    user = User(chatMessageReceiver.sender)
                ),
                url = chatMessageReceiver.url,
                isSeen = true,
                created = chatMessageReceiver.created,
                messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT_RECEIVED,
                isSync = true,
                question_id = null
            )

            if (messageObject is BaseMediaMessage)
                chatModel.downloadedLocalPath = messageObject.localPathUrl
            chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
            currentChatModel?.let {
                AppObjectController.appDatabase.chatDao().deleteChatMessage(currentChatModel)
            }

            AppObjectController.appDatabase.chatDao().updateChatMessage(chatModel)
            refreshViewLiveData?.postValue(chatModel)

            when (chatMessageReceiver.type) {
                BASE_MESSAGE_TYPE.TX -> AppAnalytics
                    .create(AnalyticsEvent.MESSAGE_SENT_TEXT.NAME)
                    .addParam(
                        "ChatId",
                        chatMessageReceiver.id
                    )
                    .push()
                BASE_MESSAGE_TYPE.IM -> AppAnalytics
                    .create(AnalyticsEvent.MESSAGE_SENT_IMAGE.NAME)
                    .addParam(
                        "ChatId",
                        chatMessageReceiver.id
                    ).push()
                BASE_MESSAGE_TYPE.VI -> AppAnalytics
                    .create(AnalyticsEvent.MESSAGE_SENT_VIDEO.NAME)
                    .addParam(
                        "ChatId",
                        chatMessageReceiver.id
                    ).push()
                BASE_MESSAGE_TYPE.AU -> AppAnalytics
                    .create(AnalyticsEvent.MESSAGE_SENT_AUDIO.NAME)
                    .addParam(
                        "ChatId",
                        chatMessageReceiver.id
                    ).push()
            }

        }
    }


}