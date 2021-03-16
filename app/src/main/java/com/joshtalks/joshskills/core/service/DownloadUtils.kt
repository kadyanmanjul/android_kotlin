package com.joshtalks.joshskills.core.service

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Func
import java.lang.reflect.Type
import java.util.HashMap
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

const val DOWNLOAD_OBJECT = "DownloadObject"

object DownloadUtils {

    val CHAT_MODEL_TYPE_TOKEN: Type = object : TypeToken<ChatModel>() {}.type
    val LESSON_QUESTION_TYPE_TOKEN: Type = object : TypeToken<LessonQuestion>() {}.type
    val objectFetchListener = HashMap<String, FetchListener>()
    private val executor: ExecutorService =
        JoshSkillExecutors.newCachedSingleThreadExecutor("Josh-Download Media")


    fun downloadFile(
        url: String,
        filePath: String,
        tag: String,
        chatModel: ChatModel?,
        fetchListener: FetchListener,
        isLesson: Boolean = false,
        lessonQuestion: LessonQuestion? = null
    ) {
        executor.execute {
            val request = Request(url, filePath)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = tag
            request.extras = if (isLesson) {
                Extras(
                    mapOf(
                        DOWNLOAD_OBJECT to AppObjectController.gsonMapperForLocal.toJson(
                            lessonQuestion
                        )
                    )
                )
            } else {
                Extras(
                    mapOf(
                        DOWNLOAD_OBJECT to AppObjectController.gsonMapperForLocal.toJson(
                            chatModel
                        )
                    )
                )
            }
            AppObjectController.getFetchObject().addListener(fetchListener)
            objectFetchListener[tag] = fetchListener
            AppObjectController.getFetchObject().remove(request.id)
            AppObjectController.getFetchObject().enqueue(
                request, Func {
                    updateDownloadStatus(
                        filePath = it.file,
                        extras = it.extras,
                        isLesson = isLesson
                    )
                },
                Func {
                    it.throwable?.printStackTrace()
                    request.tag?.let { tag ->
                        objectFetchListener[tag]?.let { it1 ->
                            AppObjectController.getFetchObject().removeListener(it1)
                        }
                    }
                })
        }
    }


    fun downloadFile(
        url: String,
        filePath: String,
        tag: String,
        fetchListener: FetchListener
    ) {
        executor.execute {
            val request = Request(url, filePath)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = tag

            AppObjectController.getFetchObject().addListener(fetchListener)
            objectFetchListener[tag] = fetchListener
            AppObjectController.getFetchObject().remove(request.id)
            AppObjectController.getFetchObject().enqueue(
                request, Func {
                    updateDownloadStatus(it.file, it.extras)
                },
                Func {
                    it.throwable?.printStackTrace()
                    request.tag?.let { tag ->
                        objectFetchListener[tag]?.let { it1 ->
                            AppObjectController.getFetchObject().removeListener(it1)
                        }
                    }
                })
        }
    }

    fun removeCallbackListener(tag: String?) {
        tag?.let { tagg ->
            objectFetchListener[tagg]?.let { it1 ->
                AppObjectController.getFetchObject().removeListener(it1)
            }

        }

    }

    fun updateDownloadStatus(
        filePath: String,
        extras: Extras,
        isLesson: Boolean = false,
        callBack: (() -> Unit)? = null,
    ) {
        executor.execute {

            try {
                if (isLesson) {
                    val lessonQuestion =
                        AppObjectController.gsonMapperForLocal.fromJson<LessonQuestion>(
                            extras.map[DOWNLOAD_OBJECT],
                            LESSON_QUESTION_TYPE_TOKEN
                        )
                    lessonQuestion.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                    if (lessonQuestion.type == LessonQuestionType.Q) {
                        lessonQuestion?.let { question ->
                            when (question.materialType) {
                                LessonMaterialType.IM ->
                                    question.imageList?.get(0).let { imageType ->
                                        imageType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateImageObject(imageType!!)

                                    }
                                LessonMaterialType.VI ->
                                    question.videoList?.get(0).let { videoType ->
                                        videoType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateVideoObject(videoType!!)

                                    }

                                LessonMaterialType.AU ->
                                    question.audioList?.get(0).let { audioType ->
                                        audioType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateAudioObject(audioType!!)

                                    }

                                LessonMaterialType.PD ->
                                    question.pdfList?.get(0).let { pdfType ->
                                        pdfType?.questionId = question.id
                                        pdfType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updatePdfObject(pdfType!!)

                                    }
                                else -> return@let
                            }
                        }

                    } else {
                        lessonQuestion.downloadedLocalPath = filePath
                    }
                    appDatabase.lessonQuestionDao()
                        .insertQuestionForLessonOnAnyThread(lessonQuestion)
                    callBack?.invoke()
                } else {
                    val chatModel = AppObjectController.gsonMapperForLocal.fromJson<ChatModel>(
                        extras.map[DOWNLOAD_OBJECT],
                        CHAT_MODEL_TYPE_TOKEN
                    )
                    chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                    if (chatModel.type == BASE_MESSAGE_TYPE.Q || chatModel.type == BASE_MESSAGE_TYPE.AR) {
                        chatModel.question?.let { question ->
                            when (question.material_type) {
                                BASE_MESSAGE_TYPE.IM ->
                                    question.imageList?.get(0).let { imageType ->
                                        imageType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateImageObject(imageType!!)

                                    }
                                BASE_MESSAGE_TYPE.VI ->
                                    question.videoList?.get(0).let { videoType ->
                                        videoType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateVideoObject(videoType!!)

                                    }
                                BASE_MESSAGE_TYPE.AU ->
                                    question.audioList?.get(0).let { audioType ->
                                        audioType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updateAudioObject(audioType!!)

                                    }
                                BASE_MESSAGE_TYPE.PD ->
                                    question.pdfList?.get(0).let { pdfType ->
                                        pdfType?.questionId = question.questionId
                                        pdfType?.downloadedLocalPath = filePath
                                        appDatabase.chatDao().updatePdfObject(pdfType!!)

                                    }
                                else -> return@let
                            }
                        }

                    } else {
                        chatModel.downloadedLocalPath = filePath
                    }
                    appDatabase.chatDao().updateChatMessageOnAnyThread(chatModel)
                    callBack?.invoke()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun downloadImage(
        viewHolder: BaseChatViewHolder,
        message: ChatModel,
        imageUrl: String,
        destPath: String
    ) {
        executor.execute {

            try {
                val extras = Extras(
                    mapOf(
                        DOWNLOAD_OBJECT to AppObjectController.gsonMapperForLocal.toJson(
                            message
                        )
                    )
                )
                val imageBitmap = Glide.with(AppObjectController.joshApplication)
                    .asBitmap()
                    .load(imageUrl).submit().get()

                val filePath = Utils.writeBitmapIntoFile(imageBitmap, destPath)
                updateDownloadStatus(filePath, extras, isLesson = false, null).let {
                    RxBus2.publish(DownloadCompletedEventBus(viewHolder, message))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun downloadAudioFile(listAudioData: List<AudioType>) {

        executor.execute {
            try {
                if (checkStoragePermission().not()) {
                    return@execute
                }
                for (audioType in listAudioData) {
                    audioType.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                    appDatabase.chatDao().updateAudioObject(audioType)
                    val file = AppDirectory.getAudioReceivedFile(audioType.audio_url).absolutePath
                    if (audioType.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                        return@execute
                    }

                    val request = Request(audioType.audio_url, file)
                    request.priority = Priority.HIGH
                    request.networkType = NetworkType.ALL
                    request.tag = audioType.id
                    AppObjectController.getFetchObject().enqueue(request, Func {
                        audioType.downloadedLocalPath = it.file
                        audioType.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                        CoroutineScope(Dispatchers.IO).launch {
                            appDatabase.chatDao().updateAudioObject(audioType)
                        }
                        objectFetchListener.remove(it.tag)
                        Timber.e(it.url + "   " + it.file)
                    },
                        Func {
                            it.throwable?.printStackTrace()
                            audioType.downloadStatus = DOWNLOAD_STATUS.FAILED
                            appDatabase.chatDao().updateAudioObject(audioType)
                        })
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            AppObjectController.joshApplication,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) + ContextCompat.checkSelfPermission(
            AppObjectController.joshApplication,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}
