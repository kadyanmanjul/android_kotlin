package com.joshtalks.joshskills.ui.chat

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.videotranscoder.MediaTranscoder
import com.joshtalks.joshskills.core.videotranscoder.format.MediaFormatStrategyPresets
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.util.AudioRecording
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage

import kotlinx.coroutines.async
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.*


class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private var compositeDisposable = CompositeDisposable()

    var recordFile = AppDirectory.tempRecordingWavFile()
    var lastChatTime: Date? = null


    init {
        addObserver()
    }

    fun startRecord() {
        AudioRecording.audioRecording.startPlayer(recordFile)
    }

    fun stopRecording() {
        AudioRecording.audioRecording.stopPlaying()

    }


    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    lateinit var inboxEntity: InboxEntity

    val chatObservableLiveData: MutableLiveData<List<ChatModel>> = MutableLiveData()

    val chatDatabseInsertLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val refreshViewLiveData: MutableLiveData<ChatModel> = MutableLiveData()


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }


    private fun addObserver() {

        compositeDisposable.add(RxBus2.listen(DBInsertion::class.java).subscribe {
            getUserRecentChats()
        })

    }


    private fun getUserRecentChats() = viewModelScope.launch(Dispatchers.IO) {

        val listOfChat: List<ChatModel> = if (lastChatTime != null) {
            appDatabase.chatDao().getRecentChatAfterTime(lastChatTime)
        } else {
            appDatabase.chatDao().getLastChats()
        }
        if (listOfChat.isNotEmpty()) {
            lastChatTime = listOfChat.last().created
        }
        val chatReturn: MutableList<ChatModel> = mutableListOf()
        for (chat in listOfChat) {
            val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
            if (question != null) {
                when {
                    question.material_type == BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            appDatabase.chatDao()
                                .getImagesOfQuestion(questionId = question.questionId)
                    question.material_type == BASE_MESSAGE_TYPE.VI -> question.videoList =
                        appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.questionId)
                    question.material_type == BASE_MESSAGE_TYPE.AU -> question.audioList =
                        appDatabase.chatDao()
                            .getAudiosOfQuestion(questionId = question.questionId)

                    question.material_type == BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        appDatabase.chatDao()
                            .getPdfOfQuestion(questionId = question.questionId)
                }
                chat.question = question
            }
            chatReturn.add(chat)
        }

        chatObservableLiveData.postValue(chatReturn)

    }


    fun sendTextMessage(messageObject: BaseChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                messageObject.conversation = inboxEntity.conversation_id
                val responseChat =
                    AppObjectController.chatNetworkService.sendMessage(messageObject).await()
                NetworkRequestHelper.updateChat(responseChat, refreshViewLiveData, messageObject)

            } catch (ex: Exception) {

                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }
    }


    fun uploadMedia(mediaPath: String, messageObject: BaseChatMessage) {
        compressMedia(mediaPath, messageObject)
    }

    private fun compressMedia(mediaPath: String, messageObject: BaseChatMessage) {

        uploadCompressedMedia(mediaPath, messageObject)

        return
        val listener = object : MediaTranscoder.Listener {
            override fun onTranscodeProgress(progress: Double) {
            }

            override fun onTranscodeCompleted() {
                Log.e("mediaa","onTranscodeCompleted"+"  "+mediaPath)
                uploadCompressedMedia(mediaPath, messageObject)
            }

            override fun onTranscodeCanceled() {
                Log.e("mediaa","onTranscodeCanceled"+"  "+mediaPath)

            }

            override fun onTranscodeFailed(exception: Exception) {
                exception.printStackTrace()
                uploadCompressedMedia(mediaPath, messageObject)
                Log.e("mediaa","onTranscodeFailed"+"  "+mediaPath)


            }
        }
        MediaTranscoder.getInstance().transcodeVideo(
            mediaPath,
            mediaPath,
            MediaFormatStrategyPresets.createAndroid720pStrategy(1200 * 1024, 96 * 1000, 1),
            listener
        )
    }

    private fun uploadCompressedMedia(mediaPath: String, messageObject: BaseChatMessage) {

        viewModelScope.launch(Dispatchers.IO) {

            try {
                val obj = mapOf("media_path" to File(mediaPath).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    (messageObject as BaseMediaMessage).url = url
                    sendTextMessage(messageObject)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return viewModelScope.async(Dispatchers.IO) {


            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }


    fun getAllUserMessage() {
        getUserRecentChats()
        Handler().postDelayed({
            val arguments = mutableMapOf<String, String>()
            arguments["created"] =
                (PrefManager.getLongValue(CHAT_LAST_SYNC_TIME) / 1000).toString()
            NetworkRequestHelper.getUpdatedChat(inboxEntity, queryMap = arguments)
        }, 500)

    }


}
