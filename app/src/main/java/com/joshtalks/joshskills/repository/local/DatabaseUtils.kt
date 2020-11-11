package com.joshtalks.joshskills.repository.local

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.repository.local.entity.CertificationExamDetailModel
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ExecutorService

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatModel =
                    AppObjectController.gsonMapperForLocal.fromJson(objs, ChatModel::class.java)
                AppObjectController.appDatabase.chatDao()
                    .updateDownloadVideoStatus(chatModel, downloadStatus)

            } catch (ex: Exception) {

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