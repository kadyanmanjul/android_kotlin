package com.joshtalks.joshskills.ui.chat

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.recorder.AudioRecording
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.repository.service.SyncChatService
import id.zelory.compressor.Compressor
import java.io.File
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody


class ConversationViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private var inboxEntity: InboxEntity
) :
    AndroidViewModel(application) {
    lateinit var recordFile: File
    private var context: JoshApplication = getApplication()
    private var appDatabase = AppObjectController.appDatabase
    private var chatDao = AppObjectController.appDatabase.chatDao()
    private var mRefreshControl = true
    private val mAudioRecording: AudioRecording = AudioRecording()
    private var isRecordingStarted = false
    private val jobs = arrayListOf<Job>()
    val conversationList: MutableList<ChatModel> = ArrayList()
    val userUnreadCourseChat = MutableSharedFlow<List<ChatModel>>()
    val userReadCourseChat = MutableSharedFlow<List<ChatModel>>()
    val oldMessageCourse = MutableSharedFlow<List<ChatModel>>()
    val updateChatMessage = MutableSharedFlow<ChatModel>()

    val refreshViewLiveData: MutableLiveData<ChatModel> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val unreadMessageCount: MutableLiveData<Int> = MutableLiveData()


    inner class CheckConnectivity : BroadcastReceiver() {
        override fun onReceive(context: Context, arg1: Intent) {
            if (Utils.isInternetAvailable()) {
                SyncChatService.syncChatWithServer(refreshViewLiveData)
            }
        }
    }


    fun sendTextMessage(messageObject: BaseChatMessage, chatModel: ChatModel?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Utils.isInternetAvailable().not()) {
                    chatModel?.let {
                        it.conversationId = inboxEntity.conversation_id
                        DatabaseUtils.addChat(it)
                    }
                }
                messageObject.conversation = inboxEntity.conversation_id
                val receiverObj =
                    AppObjectController.chatNetworkService.sendMessageAsync(messageObject)
                chatModel?.chatId = receiverObj.id
                chatModel?.created = receiverObj.created
                chatModel?.conversationId = receiverObj.conversationId
                chatModel?.isSync = true
                chatModel?.messageDeliverStatus = MESSAGE_DELIVER_STATUS.SENT_RECEIVED
                chatModel?.downloadStatus = DOWNLOAD_STATUS.UPLOADED
                chatModel?.let {
                    chatDao.updateChatMessage(chatModel)
                    delay(500)
                    refreshMessageObject(chatModel.chatId)
                }
            } catch (ex: Exception) {
                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }
    }

    fun sendMediaMessage(
        mediaPath: String,
        messageObject: BaseChatMessage,
        chatModel: ChatModel
    ) {
        chatModel.conversationId = inboxEntity.conversation_id
        viewModelScope.launch(Dispatchers.IO) {
            var compressImagePath = mediaPath
            if (chatModel.type == BASE_MESSAGE_TYPE.IM) {
                compressImagePath = getCompressImage(mediaPath)
            }
            chatModel.downloadedLocalPath = compressImagePath
            if (Utils.isInternetAvailable().not()) {
                DatabaseUtils.addChat(chatModel)
            }
            uploadCompressedMedia(compressImagePath, messageObject, chatModel)
        }
    }


    private fun uploadCompressedMedia(
        mediaPath: String,
        messageObject: BaseChatMessage,
        chatModel: ChatModel?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val obj = mapOf("media_path" to File(mediaPath).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    (messageObject as BaseMediaMessage).url = url
                    sendTextMessage(messageObject, chatModel)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private suspend fun getCompressImage(path: String): String {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                AppDirectory.copy(
                    Compressor(getApplication()).setQuality(75).setMaxWidth(720).setMaxHeight(
                        1280
                    ).compressToFile(File(path)).absolutePath, path
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@async path
        }.await()
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

            try {
                val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                    responseObj.url,
                    parameters,
                    body
                ).execute()
                return@async responseUpload.code()

            } catch (ex: Exception) {
                return@async 220
            }
        }.await()
    }


    fun getAllCourseMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            val totalMessage = chatDao.getTotalCountOfRows(inboxEntity.conversation_id)
            if (totalMessage == 0L) {
                getNewMessageFromServer(delayTimeNextRequest = 500)
                return@launch
            }

            val lastUnreadMessage = chatDao.getLastUnreadReadMessage(inboxEntity.conversation_id)
            if (lastUnreadMessage == null) {
                userReadCourseChat.emit(
                    chatDao.getOneShotMessageList(inboxEntity.conversation_id)
                        .sortedWith(compareBy({ it.created }, { it.getMsTime() }))
                )
            } else {
                userReadCourseChat.emit(
                    chatDao.getPagingMessage(
                        inboxEntity.conversation_id,
                        lastUnreadMessage.created.time
                    ).sortedWith(compareBy({ it.created }, { it.getMsTime() }))
                )

                userUnreadCourseChat.emit(
                    chatDao.getUnreadMessageList(
                        inboxEntity.conversation_id,
                        lastUnreadMessage.created.time
                    ).sortedWith(compareBy({ it.created }, { it.getMsTime() }))
                )
            }
            updateAllMessageReadByUser()
            getNewMessageFromServer()
        }
    }


    fun loadPagingMessage(lastMessage: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            oldMessageCourse.emit(
                chatDao.getPagingMessage(
                    inboxEntity.conversation_id,
                    lastMessage.created.time
                )
                    .sortedWith(compareBy({ it.created }, { it.getMsTime() }))
            )
            updateAllMessageReadByUser()
        }
    }

    fun addNewMessages(lastMessageTime: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (lastMessageTime > 0L) {
                delay(250)
            }
            userUnreadCourseChat.emit(
                chatDao.getNewFetchMessages(
                    inboxEntity.conversation_id, lastMessageTime
                ).sortedWith(compareBy({ it.created }, { it.getMsTime() }))
            )
            updateAllMessageReadByUser()
        }
    }

    private fun updateAllMessageReadByUser() {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.chatDao().readAllChatBYUser(inboxEntity.conversation_id)
        }
    }


    private fun getNewMessageFromServer(delayTimeNextRequest: Long = 0L) {
        if (Utils.isInternetAvailable()) {
            val arguments = mutableMapOf<String, String>()
            val (key, value) = PrefManager.getLastSyncTime(inboxEntity.conversation_id)
            arguments[key] = value
            jobs += NetworkRequestHelper.getUpdatedChat(
                inboxEntity.conversation_id,
                queryMap = arguments,
                courseId = inboxEntity.courseId.toInt(),
                delayTimeNextRequest = delayTimeNextRequest
            )
        } else {
            RxBus2.publish(MessageCompleteEventBus(false))
        }
    }

    fun setMRefreshControl(control: Boolean) {
        mRefreshControl = control
    }

    fun refreshChatOnManual() {
        getNewMessageFromServer()
    }


    fun refreshMessageObject(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatObj = chatDao.getUpdatedChatObjectViaId(chatId)
            updateChatMessage.emit(chatObj)
        }
    }

    fun refreshLesson(lessonId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val lessonModel = appDatabase.lessonDao().getLesson(lessonId)
            lessonModel?.let {
                refreshMessageObject(it.chatId)
            }
        }
    }


    fun getAwardMentorModel(awardMentorId: Int): AwardMentorModel? {
        return appDatabase.awardMentorModelDao().getAwardMentorModel(awardMentorId)
    }

    @Synchronized
    fun startRecord(recordListener: OnAudioRecordListener?) {
        val onRecordListener: OnAudioRecordListener = object :
            OnAudioRecordListener {
            override fun onRecordFinished(recordingItem: RecordingItem) {
                isRecordingStarted = false
                recordListener?.onRecordFinished(recordingItem)
            }

            override fun onError(e: Int) {
                recordListener?.onError(e)
            }

            override fun onRecordingStarted() {
                isRecordingStarted = true
                recordListener?.onRecordingStarted()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            AppDirectory.tempRecordingFile().let {
                mAudioRecording.setOnAudioRecordListener(onRecordListener)
                mAudioRecording.setFile(it.absolutePath)
                recordFile = it
                mAudioRecording.startRecording()
            }
        }
    }

    fun isRecordingStarted(): Boolean {
        return isRecordingStarted
    }

    @Synchronized
    fun stopRecording(cancel: Boolean) {
        mAudioRecording.stopRecording(cancel)
        isRecordingStarted = false
    }


    override fun onCleared() {
        super.onCleared()
        if (isRecordingStarted) {
            mAudioRecording.stopRecording(true)
        }
    }

}
