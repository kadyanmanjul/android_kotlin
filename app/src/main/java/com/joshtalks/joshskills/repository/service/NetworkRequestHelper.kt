package com.joshtalks.joshskills.repository.service

import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.server.ChatMessageReceiver
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object NetworkRequestHelper {

    fun getUpdatedChat(
        conversationId: String,
        queryMap: Map<String, String> = emptyMap()
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = AppObjectController.chatNetworkService.getUnReceivedMessageAsync(
                    conversationId,
                    queryMap
                )
                if (resp.chatModelList.isNullOrEmpty()) {
                    RxBus2.publish(MessageCompleteEventBus(false))
                } else {
                    PrefManager.put(
                        conversationId.trim(),
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
                        question.tempType = question.type
                        AppObjectController.appDatabase.chatDao().insertChatQuestion(question)
                        question.audioList?.let {
                            it.listIterator().forEach { audioType ->
                                audioType.questionId = question.questionId
                                //  DownloadUtils.downloadAudioFile(it)
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
                                videoType.interval = question.interval
                            }
                            AppObjectController.appDatabase.chatDao().insertVideoMessageList(it)
                        }

                        question.lesson?.let {
                            AppObjectController.appDatabase.lessonDao().insertSingleItem(it)
                        }
                        question.practiseEngagementV2?.forEach { pe ->
                            pe.questionForId = question.questionId
                            AppObjectController.appDatabase.practiceEngagementDao()
                                .insertPractise(pe)
                        }

                        if (question.type == BASE_MESSAGE_TYPE.CE) {
                            question.certificateExamId?.run {
                                DatabaseUtils.getCExamDetails(
                                    conversationId = conversationId,
                                    certificationId = this
                                )
                            }
                        }
                    }
                    chatModel.awardMentorModel?.let { awardMentorModel->
                        AppObjectController.appDatabase.awardMentorModelDao().insertSingleItem(awardMentorModel)
                    }
                }
                RxBus2.publish(DBInsertion("Chat"))

                resp.next?.let {
                    val arguments = mutableMapOf<String, String>()
                    PrefManager.getLastSyncTime(conversationId).let { keys ->
                        arguments[keys.first] = keys.second
                    }
                    getUpdatedChat(conversationId, queryMap = arguments)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    suspend fun isVideoPresentInUpdatedChat(
        conversationId: String,
        queryMap: Map<String, String> = emptyMap()
    ): VideoType? {
        var videoType1: VideoType? = null
        try {
            val resp = AppObjectController.chatNetworkService.getUnReceivedMessageAsync(
                conversationId,
                queryMap
            )
            if (resp.chatModelList.isNullOrEmpty()) {
            } else {
                PrefManager.put(
                    conversationId.trim(),
                    resp.chatModelList.last().messageTimeInMilliSeconds
                )
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
                            // DownloadUtils.downloadAudioFile(it)
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

                    question.lesson?.let {
                        AppObjectController.appDatabase.lessonDao().insertSingleItem(it)

                    }

                    question.videoList?.let {
                        it.listIterator().forEach { videoType ->
                            videoType.questionId = question.questionId
                            videoType.downloadStatus = DOWNLOAD_STATUS.NOT_START
                            videoType.interval = question.interval
                            videoType1 = videoType
                        }
                        AppObjectController.appDatabase.chatDao().insertVideoMessageList(it)
                    }
                }

                chatModel.awardMentorModel?.let { awardMentorModel->
                    AppObjectController.appDatabase.awardMentorModelDao().insertSingleItem(awardMentorModel)
                }
            }

            resp.next?.let {
                val arguments = mutableMapOf<String, String>()
                PrefManager.getLastSyncTime(conversationId).let { keys ->
                    arguments[keys.first] = keys.second
                }
                videoType1 = isVideoPresentInUpdatedChat(conversationId, queryMap = arguments)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return videoType1
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
                else -> {
                    return@launch
                }
            }
        }
    }
}