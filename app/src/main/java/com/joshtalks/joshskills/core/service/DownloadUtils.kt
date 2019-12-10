package com.joshtalks.joshskills.core.service

import android.util.Log
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.joshtalks.joshskills.core.AppObjectController.Companion.fetch
import com.joshtalks.joshskills.core.Utils
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.Func;
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.tonyodev.fetch2core.Extras
import kotlinx.coroutines.async
import java.lang.reflect.Type
import java.util.HashMap


const val DOWNLOAD_OBJECT = "DownloadObject"

object DownloadUtils {

    private val CHAT_MODEL_TYPE_TOKEN: Type = object : TypeToken<ChatModel>() {}.type

    val objectFetchListener = HashMap<String, FetchListener>()


    fun downloadFile(
        url: String,
        filePath: String,
        tag: String,
        chatModel: ChatModel,
        fetchListener: FetchListener
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = Request(url, filePath)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = tag
            request.extras =
                Extras(
                    mapOf(
                        DOWNLOAD_OBJECT to AppObjectController.gsonMapperForLocal.toJson(
                            chatModel
                        )
                    )
                )
            fetch.addListener(fetchListener)
            objectFetchListener[tag] = fetchListener
            //request.toDownloadInfo()
            fetch.enqueue(request, Func {
                CoroutineScope(Dispatchers.IO).launch {
                    updateDownloadStatus(it.file, it.extras)
                }

            },
                Func {
                    it.throwable?.printStackTrace()
                    request.tag?.let { tag ->
                        objectFetchListener[tag]?.let { it1 -> fetch.removeListener(it1) }
                    }
                })
        }
    }

    fun removeCallbackListener(tag: String?) {
        tag?.let { tagg ->
            objectFetchListener[tagg]?.let { it1 ->
                fetch.removeListener(it1)
            }

        }

    }

    suspend fun updateDownloadStatus(filePath: String, extras: Extras): Boolean {

        return CoroutineScope(Dispatchers.IO).async {
            val chatModel = AppObjectController.gsonMapperForLocal.fromJson<ChatModel>(
                extras.map[DOWNLOAD_OBJECT],
                CHAT_MODEL_TYPE_TOKEN
            )
            chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED

            if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
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
                                pdfType?.downloadedLocalPath = filePath
                                appDatabase.chatDao().updatePdfObject(pdfType!!)

                            }
                        else ->return@let
                    }
                }

            } else {
                chatModel.downloadedLocalPath = filePath
            }
            appDatabase.chatDao().updateChatMessage(chatModel)
            return@async true
        }.await()

    }

    fun downloadImage(
        viewHolder: BaseChatViewHolder,
        message: ChatModel,
        imageUrl: String,
        destPath: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
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
                updateDownloadStatus(filePath, extras).let {
                    RxBus2.publish(DownloadCompletedEventBus(viewHolder, message))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }


    fun downloadAudioFile(listAudioData: List<AudioType>) = CoroutineScope(Dispatchers.IO).launch {
        for (audioType in listAudioData) {

            audioType.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
            appDatabase.chatDao().updateAudioObject(audioType)
            val file = AppDirectory.getRecordingReceivedFilePath()
            if (audioType.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                return@launch
            }

            Log.e("file url", file + "   " + audioType.audio_url)

            val request = Request(audioType.audio_url, file)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.tag = audioType.id



            fetch.enqueue(request, Func {
                audioType.downloadedLocalPath = it.file
                audioType.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                CoroutineScope(Dispatchers.IO).launch {
                    appDatabase.chatDao().updateAudioObject(audioType)
                }
                objectFetchListener.remove(it.tag)
                Log.e("file url", it.url + "   " + it.file)


            },
                Func {
                    it.throwable?.printStackTrace()
                    audioType.downloadStatus = DOWNLOAD_STATUS.FAILED
                    CoroutineScope(Dispatchers.IO).launch {
                        appDatabase.chatDao().updateAudioObject(audioType)
                    }

                })
        }
    }


}