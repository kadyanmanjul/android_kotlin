package com.joshtalks.joshskills.repository.local

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.eventbus.VideoDownloadedBus
import com.joshtalks.joshskills.repository.local.eventbus.VideoDownloadedBusForLessonQuestion
import java.util.*
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object DatabaseUtils {
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-LastUsed")

    fun updateUserMessageSeen() {
        executor.execute {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.HOUR, -1)
            val oneHourBack = cal.time
            AppObjectController.appDatabase.chatDao().updateSeenMessages(compareTime = oneHourBack)
        }
    }

    suspend fun addChat(chatModel: ChatModel): Long {
        return CoroutineScope(Dispatchers.IO).async {
            val cal = Calendar.getInstance()
            cal.time = Date(cal.time.time)
            chatModel.isSync = false
            chatModel.created = cal.time
            chatModel.isSeen = true
            chatModel.chatLocalId?.let {
                chatModel.chatId = it
            }
            return@async AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
        }.await()

    }

    @JvmStatic
    fun updateVideoDownload(objs: String, downloadStatus: DOWNLOAD_STATUS) {
        executor.execute {
            try {
                val chatModel =
                    AppObjectController.gsonMapperForLocal.fromJson(objs, ChatModel::class.java)
                AppObjectController.appDatabase.chatDao()
                    .updateDownloadStatus(chatModel.chatId, downloadStatus)
                if (downloadStatus == DOWNLOAD_STATUS.FAILED || downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                    RxBus2.publish(VideoDownloadedBus(chatModel))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            try {
                val lessonQuestion =
                    AppObjectController.gsonMapperForLocal.fromJson(
                        objs,
                        LessonQuestion::class.java
                    )
                AppObjectController.appDatabase.lessonQuestionDao()
                    .updateDownloadStatus(lessonQuestion.id, downloadStatus)
                if (downloadStatus == DOWNLOAD_STATUS.FAILED || downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                    RxBus2.publish(VideoDownloadedBusForLessonQuestion(lessonQuestion))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

    }

    @JvmStatic
    fun updateAllVideoStatusWhichIsDownloading() {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.chatDao().updateDownloadVideoStatusFailed()
        }
    }

    @JvmStatic
    fun updateVideoProgress(json: String, progress: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.appDatabase.chatDao().videoProgressUpdate(
                    AppObjectController.gsonMapperForLocal.fromJson(
                        json,
                        ChatModel::class.java
                    ).conversationId, progress
                )
            } catch (ex: Exception) {
            }
        }
    }

    fun updateLastUsedModification(conversationId: String) {
        executor.execute {
            AppObjectController.appDatabase.chatDao().lastUsedBy(conversationId)
        }
    }

    fun getCExamDetails(
        conversationId: String,
        certificationId: Int,
        callback: ((CertificationExamDetailModel) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = mapOf(
                    "conversation_id" to conversationId,
                    "certificateexam_id" to certificationId.toString()
                )
                val response =
                    AppObjectController.chatNetworkService.getCertificateExamCardDetails(params)
                callback?.invoke(response)
                AppObjectController.appDatabase.chatDao()
                    .insertCertificateExamDetail(certificationId, response)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }


}