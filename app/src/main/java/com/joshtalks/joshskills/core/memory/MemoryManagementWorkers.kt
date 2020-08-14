package com.joshtalks.joshskills.core.memory

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.bytesToKB
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import timber.log.Timber
import java.io.File

class RemoveMediaWorker(var context: Context, var workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val appDatabase = AppObjectController.appDatabase
        val conversationId = workerParams.inputData.getString("conversation_id") ?: EMPTY
        val timeDelete = workerParams.inputData.getBoolean("time_delete", false)

        val minimumLimit = AppObjectController.getFirebaseRemoteConfig()
            .getLong("MINIMUM_STORAGE_REMOVE_IN_MB") * 1024
        var totalDeletedMediaSize = 0.0


        val listOfChat: List<ChatModel> = if (timeDelete) {
            appDatabase.chatDao().getAllRecentDownloadMedia()
        } else {
            appDatabase.chatDao().getLastChats(conversationId)
        }
        for ((index, chat) in listOfChat.withIndex()) {
            if (timeDelete && totalDeletedMediaSize >= minimumLimit) {
                break
            }
            val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
            question?.run {
                when (material_type) {
                    BASE_MESSAGE_TYPE.VI -> {
                        appDatabase.chatDao().getVideosOfQuestion(questionId = question.questionId)
                            .getOrNull(0)?.run {
                                totalDeletedMediaSize +=
                                    AppObjectController.videoDownloadTracker.downloadMediaSize(
                                        this.video_url
                                    ).bytesToKB()
                                AppObjectController.videoDownloadTracker.removeDownload(
                                    Uri.parse(
                                        this.video_url ?: EMPTY
                                    )
                                )
                                deleteLocalCreatedFile(downloadedLocalPath)
                                chat.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                appDatabase.chatDao().updateChatMessage(chat)
                                this.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                appDatabase.chatDao().updateVideoObject(this)
                            }
                    }
                    else -> {
                        if (PermissionUtils.isStoragePermissionEnabled(applicationContext)) {
                            when (material_type) {
                                BASE_MESSAGE_TYPE.AU ->
                                    appDatabase.chatDao()
                                        .getAudiosOfQuestion(questionId = question.questionId)
                                        .getOrNull(0)
                                        ?.run {
                                            AppDirectory.getAudioReceivedFile(audio_url).run {
                                                totalDeletedMediaSize += AppDirectory.getFileSize(
                                                    this
                                                ).bytesToKB()
                                                AppDirectory.deleteFileFile(this)
                                            }
                                            deleteLocalCreatedFile(downloadedLocalPath)
                                            chat.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            appDatabase.chatDao().updateChatMessage(chat)
                                            downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            downloadedLocalPath = null
                                            appDatabase.chatDao().updateAudioObject(this)
                                        }
                                BASE_MESSAGE_TYPE.IM ->
                                    appDatabase.chatDao()
                                        .getImagesOfQuestion(questionId = question.questionId)
                                        .getOrNull(0)
                                        ?.run {
                                            downloadedLocalPath?.let {
                                                totalDeletedMediaSize += AppDirectory.getFileSize(
                                                    File(it)
                                                )
                                                    .bytesToKB()
                                            }
                                            AppDirectory.deleteFile(this.downloadedLocalPath)
                                            chat.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            appDatabase.chatDao().updateChatMessage(chat)
                                            this.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            appDatabase.chatDao().updateImageObject(this)
                                        }
                                BASE_MESSAGE_TYPE.PD ->
                                    appDatabase.chatDao()
                                        .getPdfOfQuestion(questionId = question.questionId)
                                        .getOrNull(0)
                                        ?.run {
                                            AppDirectory.docsReceivedFile(url).run {
                                                totalDeletedMediaSize += AppDirectory.getFileSize(
                                                    this
                                                )
                                                    .bytesToKB()
                                                AppDirectory.deleteFileFile(this)
                                            }
                                            chat.downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            appDatabase.chatDao().updateChatMessage(chat)
                                            downloadStatus = DOWNLOAD_STATUS.NOT_START
                                            downloadedLocalPath = null
                                            appDatabase.chatDao().updatePdfObject(this)
                                        }
                                else -> {
                                }

                            }
                        } else {
                            return@run
                        }
                    }
                }
            }
            Timber.tag("Storage").e("" + totalDeletedMediaSize + "  " + index)
        }
        return Result.success()
    }

    private fun deleteLocalCreatedFile(path: String?) {
        path?.run {
            AppDirectory.getFileSize(File(this))
            AppDirectory.deleteFile(this)
        }
    }
}


class MemoryManagementWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val limit = AppObjectController.getFirebaseRemoteConfig()
                .getLong("MAXIMUM_STORAGE_IN_MB") * 1024
            var externalFileSize = 0.0
            context.getExternalFilesDir(null)?.run {
                externalFileSize = AppDirectory.getDirSize(this).bytesToKB()
            }

            var cacheFileSize = 0.0
            context.cacheDir?.run {
                cacheFileSize = AppDirectory.getDirSize(this).bytesToKB()
            }

            var externalStorageSize = 0.0
            if (PermissionUtils.isStoragePermissionEnabled(applicationContext)) {
                externalStorageSize =
                    AppDirectory.getDirSize(AppDirectory.getRootDirectoryPath()).bytesToKB()
            }


            val totalUsedInMB = externalFileSize + cacheFileSize + externalStorageSize
            Timber.tag("Storage")
                .e(
                    "a = " + externalFileSize
                            + "  b = " + cacheFileSize
                            + "  c = " + externalStorageSize
                            + "  totalUsedInKB = " + totalUsedInMB
                )

            if (totalUsedInMB >= limit) {
                removeOldMedia()
                cacheClearOfGlide()
            } else {
                if (cacheFileSize > 200) {
                    cacheClearOfGlide()
                }
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }

    private fun cacheClearOfGlide() {
        Glide.get(context).clearDiskCache()
        //Glide.get(context).trimMemory(TRIM_MEMORY_RUNNING_CRITICAL)
    }

    private fun removeOldMedia() {
        WorkMangerAdmin.clearMediaOfConversation(EMPTY, isTimeDelete = true)
    }


}