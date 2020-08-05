package com.joshtalks.joshskills.ui.chat

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.joshtalks.joshskills.util.AudioRecording
import id.zelory.compressor.Compressor
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.jetbrains.anko.collections.forEachWithIndex
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


class ConversationViewModel(application: Application) :
    AndroidViewModel(application) {
    lateinit var inboxEntity: InboxEntity
    private var compositeDisposable = CompositeDisposable()
    lateinit var recordFile: File
    private var lastChatTime: Date? = null
    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val chatObservableLiveData: MutableLiveData<List<ChatModel>> = MutableLiveData()
    val refreshViewLiveData: MutableLiveData<ChatModel> = MutableLiveData()
    private var lastMessageTime: Date? = null
    private var broadCastForNetwork = CheckConnectivity()
    private var mRefreshControl = true

    init {
        addObserver()
    }

    fun startRecord(): Boolean {
        AppDirectory.tempRecordingFile().let {
            recordFile = it
            AudioRecording.audioRecording.startPlayer(recordFile)
            return@let true
        }
        return false
    }

    fun stopRecording() {
        AudioRecording.audioRecording.stopPlaying()
    }


    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        context.unregisterReceiver(broadCastForNetwork)
    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(DBInsertion::class.java).subscribeOn(Schedulers.computation()).subscribe {
                getUserRecentChats()
            })
        val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(
            broadCastForNetwork,
            filter
        )       //TODO(FixMe) - ViewModel should not hold any reference of Context
    }

    inner class CheckConnectivity : BroadcastReceiver() {

        override fun onReceive(context: Context, arg1: Intent) {
            if (Utils.isInternetAvailable()) {
                SyncChatService.syncChatWithServer(refreshViewLiveData)
            }
        }
    }


    fun getUserRecentChats() = viewModelScope.launch(Dispatchers.IO) {

        val chatReturn: MutableList<ChatModel> = mutableListOf()

        val listOfChat: List<ChatModel> = if (lastChatTime != null) {
            appDatabase.chatDao().getRecentChatAfterTime(inboxEntity.conversation_id, lastChatTime)
        } else {
            appDatabase.chatDao().getLastChats(inboxEntity.conversation_id)
        }
        if (listOfChat.isNotEmpty()) {
            lastChatTime = listOfChat.last().created
        }
        listOfChat.forEachWithIndex { _, chat ->
            val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
            question?.run {
                when (this.material_type) {
                    BASE_MESSAGE_TYPE.IM -> question.imageList =
                        appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI -> question.videoList =
                        appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU -> question.audioList =
                        appDatabase.chatDao()
                            .getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        appDatabase.chatDao()
                            .getPdfOfQuestion(questionId = question.questionId)
                }
                chat.question = question
                if (this.parent_id.isNullOrEmpty().not()) {
                    chat.parentQuestionObject =
                        appDatabase.chatDao().getQuestionOnId(this.parent_id!!)
                }
            }

            if (chat.type == BASE_MESSAGE_TYPE.Q && question == null) {
                return@forEachWithIndex
            }

            chatReturn.add(chat)
        }
        if (chatReturn.isNullOrEmpty()) {
            return@launch
        }
        lastMessageTime = chatReturn.last().created
        chatObservableLiveData.postValue(chatReturn)
        updateAllMessageReadByUser()
    }

    private fun updateAllMessageReadByUser() {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.chatDao().readAllChatBYUser(inboxEntity.conversation_id)
        }
    }


    fun sendTextMessage(messageObject: BaseChatMessage, chatModel: ChatModel?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatModel?.let {
                    it.conversationId = inboxEntity.conversation_id
                    if (Utils.isInternetAvailable().not()) {
                        DatabaseUtils.addChat(it)
                    }
                }
                messageObject.conversation = inboxEntity.conversation_id
                val responseChat =
                    AppObjectController.chatNetworkService.sendMessageAsync(messageObject).await()
                NetworkRequestHelper.updateChat(
                    responseChat,
                    refreshViewLiveData,
                    messageObject,
                    chatModel
                )
            } catch (ex: Exception) {
                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }
    }


    fun uploadMedia(mediaPath: String, messageObject: BaseChatMessage, chatModel: ChatModel) {
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
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }


    fun getAllUserMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            val rows = appDatabase.chatDao().getTotalCountOfRows(inboxEntity.conversation_id)
            getUserRecentChats()
            try {
                if (rows > 0) {
                    delay(2500)
                } else {
                    delay(250)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            if (Utils.isInternetAvailable()) {
                val arguments = mutableMapOf<String, String>()
                val (key, value) = PrefManager.getLastSyncTime(inboxEntity.conversation_id)
                arguments[key] = value
                val lastQuestionTime =
                    appDatabase.chatDao().getLastChatDate(inboxEntity.conversation_id)

                if (lastQuestionTime.isNullOrEmpty().not()) {
                    arguments.remove(key)
                    arguments["createdmilisecond"] = lastQuestionTime!!
                }

                NetworkRequestHelper.getUpdatedChat(
                    inboxEntity.conversation_id,
                    queryMap = arguments
                )
            } else {
                RxBus2.publish(MessageCompleteEventBus(false))
            }
        }
        refreshChatEverySomeTime()
    }

    fun getAllUnlockedMessage(date: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            getUserUnlockClass(date)
        }
    }

    private fun getUserUnlockClass(date: Date) = viewModelScope.launch(Dispatchers.IO) {

        val chatReturn: MutableList<ChatModel> = mutableListOf()

        val listOfChat: List<ChatModel> = if (date != null) {
            appDatabase.chatDao().getRecentChatAfterTime(inboxEntity.conversation_id, date)
        } else {
            return@launch
        }
        if (listOfChat.isNotEmpty()) {
            lastChatTime = listOfChat.last().created
        }
        listOfChat.forEachWithIndex { _, chat ->
            val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
            question?.run {
                when (this.material_type) {
                    BASE_MESSAGE_TYPE.IM -> question.imageList =
                        appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI -> question.videoList =
                        appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU -> question.audioList =
                        appDatabase.chatDao()
                            .getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        appDatabase.chatDao()
                            .getPdfOfQuestion(questionId = question.questionId)
                }
                chat.question = question
                if (this.parent_id.isNullOrEmpty().not()) {
                    chat.parentQuestionObject =
                        appDatabase.chatDao().getQuestionOnId(this.parent_id!!)
                }
            }

            if (chat.type == BASE_MESSAGE_TYPE.Q && question == null) {
                return@forEachWithIndex
            }

            chatReturn.add(chat)
        }
        if (chatReturn.isNullOrEmpty()) {
            RxBus2.publish(MessageCompleteEventBus(false))
            return@launch
        }
        lastMessageTime = chatReturn.last().created
        chatObservableLiveData.postValue(chatReturn)
        updateAllMessageReadByUser()
        RxBus2.publish(MessageCompleteEventBus(false))
    }

    fun setMRefreshControl(control: Boolean) {
        mRefreshControl = control
    }

    private fun refreshChatEverySomeTime() {
        compositeDisposable.add(
            Observable.interval(1, 1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.computation())
                .timeInterval()
                .subscribe({
                    if (Utils.isInternetAvailable() && mRefreshControl) {
                        val arguments = mutableMapOf<String, String>()
                        val (key, value) = PrefManager.getLastSyncTime(inboxEntity.conversation_id)
                        arguments[key] = value
                        NetworkRequestHelper.getUpdatedChat(
                            inboxEntity.conversation_id,
                            queryMap = arguments
                        )
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun updateInDatabaseReadMessage(readChatList: MutableSet<ChatModel>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (readChatList.isNullOrEmpty().not()) {
                    val idList = readChatList.map { it.chatId }.toMutableList()
                    appDatabase.chatDao().updateMessageStatus(MESSAGE_STATUS.SEEN_BY_USER, idList)
                }
            } catch (ex: ConcurrentModificationException) {
                ex.printStackTrace()
            }
        }

    }

    fun refreshChatOnManual() {
        val arguments = mutableMapOf<String, String>()
        val (key, value) = PrefManager.getLastSyncTime(inboxEntity.conversation_id)
        arguments[key] = value
        NetworkRequestHelper.getUpdatedChat(
            inboxEntity.conversation_id,
            queryMap = arguments
        )
    }

    suspend fun updateBatchChangeRequest() {

        val response =
            AppObjectController.chatNetworkService.changeBatchRequest(inboxEntity.conversation_id)
        if (response.isSuccessful) {
            deleteChatModelOfType(BASE_MESSAGE_TYPE.UNLOCK)
            refreshChatOnManual()
        }
    }

    suspend fun deleteChatModelOfType(type: BASE_MESSAGE_TYPE) {
        AppObjectController.appDatabase.chatDao()
            .deleteSpecificTypeChatModel(inboxEntity.conversation_id, type)
    }

    suspend fun insertUnlockClassToDatabase(unlockChatModel: ChatModel) {

        deleteChatModelOfType(BASE_MESSAGE_TYPE.UNLOCK)

        val chatObj =
            AppObjectController.appDatabase.chatDao()
                .getNullableChatObject(unlockChatModel.chatId)
        if (chatObj == null) {
            AppObjectController.appDatabase.chatDao().insertAMessage(unlockChatModel)
        } else {
            chatObj.chatId = unlockChatModel.chatLocalId.toString()
            chatObj.conversationId = inboxEntity.conversation_id
            chatObj.created = unlockChatModel.created
            chatObj.type = unlockChatModel.type
            AppObjectController.appDatabase.chatDao().updateChatMessage(chatObj)
        }
    }

}
