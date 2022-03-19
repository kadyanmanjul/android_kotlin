package com.joshtalks.joshskills.repository.service

import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object NetworkRequestHelper {
    val practiceEnagagement = object : TypeToken<List<PracticeEngagement>>() {}.type

    suspend fun getUpdatedChat(
        conversationId: String,
        queryMap: Map<String, String> = emptyMap(),
        courseId: Int,
        delayTimeNextRequest: Long = 0L,
        refreshMessageUser: Boolean = false
    ) {
        try {
            val resp = AppObjectController.chatNetworkService.getUnReceivedMessageAsync(
                conversationId,
                queryMap
            )
            if (resp.chatModelList.isNullOrEmpty()) {
                RxBus2.publish(MessageCompleteEventBus(false))
            } else {
                LastSyncPrefManager.put(
                    conversationId.trim(),
                    getTimeInString(resp.chatModelList.last().messageTime)
                )
                RxBus2.publish(MessageCompleteEventBus(true))
            }


            for (chatModel in resp.chatModelList) {
                val chatObj =
                    AppObjectController.appDatabase.chatDao()
                        .getNullableChatObject(chatModel.chatId)
                if (chatObj == null) {
                    chatModel.downloadStatus = DOWNLOAD_STATUS.NOT_START
                    chatModel.conversationId = conversationId
                    AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
                } else {
                    chatObj.chatId = chatModel.chatId
                    chatObj.url = chatModel.url
                    chatObj.isSeen = true
                    chatObj.conversationId = chatModel.conversationId
                    chatObj.created = chatModel.created
                    chatObj.messageDeliverStatus = chatModel.messageDeliverStatus
                    chatObj.type = chatModel.type
                    chatObj.conversationId = conversationId
                    AppObjectController.appDatabase.chatDao().updateChatMessage(chatObj)
                }
                if (chatModel.type == BASE_MESSAGE_TYPE.LESSON) {
                    chatModel.lesson?.apply {
                        chatId = chatModel.chatId
                    }?.let {
                        AppObjectController.appDatabase.lessonDao().insertSingleItem(it)
                        downloadImageGlide(it.thumbnailUrl)
                    }
                }
                chatModel.question?.let { question ->
                    question.chatId = chatModel.chatId
                    question.course_id = courseId

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
                            downloadImageGlide(imageType.imageUrl)
                        }
                        AppObjectController.appDatabase.chatDao().insertImageTypeMessageList(it)

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
                            downloadImageGlide(videoType.video_image_url)
                        }
                        AppObjectController.appDatabase.chatDao().insertVideoMessageList(it)
                    }

                    try {
                        if (question.practiseEngagementV2.isNullOrEmpty().not()) {
                            question.practiceEngagement =
                                AppObjectController.gsonMapper.fromJson(
                                    question.practiseEngagementV2?.toString(),
                                    practiceEnagagement
                                )
                            question.practiseEngagementV2?.forEach { pe ->
                                pe.questionForId = question.questionId
                                AppObjectController.appDatabase.practiceEngagementDao()
                                    .insertPractise(pe)
                            }
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
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

                chatModel.awardMentorModel?.let { awardMentorModel ->
                    AppObjectController.appDatabase.awardMentorModelDao()
                        .insertSingleItem(awardMentorModel)
                }

                chatModel.lesson?.let {
                    it.chatId = chatModel.chatId
                    AppObjectController.appDatabase.lessonDao().insertSingleItem(it)
                }

                chatModel.specialPractice?.let {
                    AppObjectController.appDatabase.specialDao().insertSingleItem(it)
                }
            }
            if (resp.chatModelList.isEmpty()) {
                RxBus2.publish(DBInsertion("ChatIEmpty"))
            } else {
                RxBus2.publish(DBInsertion("ChatInsert", refreshMessageUser))
            }

            resp.next?.let {
                val arguments = mutableMapOf<String, String>()
                LastSyncPrefManager.getLastSyncTime(conversationId).let { keys ->
                    arguments[keys.first] = keys.second
                }
                delay(delayTimeNextRequest)
                getUpdatedChat(
                    conversationId,
                    queryMap = arguments,
                    courseId,
                    delayTimeNextRequest = 0
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    suspend fun isVideoPresentInUpdatedChat(
        conversationId: String,
        queryMap: Map<String, String> = emptyMap()
    ): VideoType? {
        var videoType1: VideoType? = null
        try {
//            val tempMap: Map<String, String> = mapOf(Pair("created", "0"))
            val resp = AppObjectController.chatNetworkService.getUnReceivedMessageAsync(
                conversationId,
                queryMap
            )
            if (resp.chatModelList.isNullOrEmpty().not()) {
                LastSyncPrefManager.put(
                    conversationId.trim(),
                    getTimeInString(resp.chatModelList.last().messageTime)
                )
            }

            for (chatModel in resp.chatModelList) {
                val chatObj =
                    AppObjectController.appDatabase.chatDao()
                        .getNullableChatObject(chatModel.chatId)
                if (chatObj == null) {
                    chatModel.downloadStatus = DOWNLOAD_STATUS.NOT_START
                    chatModel.conversationId = conversationId
                    AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
                } else {
                    chatObj.chatId = chatModel.chatId
                    chatObj.url = chatModel.url
                    chatObj.isSeen = true
                    chatObj.conversationId = chatModel.conversationId
                    chatObj.created = chatModel.created
                    chatObj.messageDeliverStatus = chatModel.messageDeliverStatus
                    chatObj.type = chatModel.type
                    chatObj.conversationId = conversationId
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

                chatModel.awardMentorModel?.let { awardMentorModel ->
                    AppObjectController.appDatabase.awardMentorModelDao()
                        .insertSingleItem(awardMentorModel)
                }

                chatModel.lesson?.let {
                    it.chatId = chatModel.chatId
                    AppObjectController.appDatabase.lessonDao().insertSingleItem(it)
                }

            }

            resp.next?.let {
                val arguments = mutableMapOf<String, String>()
                LastSyncPrefManager.getLastSyncTime(conversationId).let { keys ->
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
                question_id = null,
                modified = chatMessageReceiver.created
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

    private fun downloadImageGlide(url: String) {
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(Priority.IMMEDIATE)
            .skipMemoryCache(true)

        Glide.with(AppObjectController.joshApplication)
            .load(url)
            .apply(
                requestOptions
            )
            .submit()
    }

    private fun getTimeInString(time: Double): String {
        return String.format("%.6f", time)
    }
}