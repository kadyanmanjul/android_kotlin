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


class ConversationViewModel(application: Application, private var inboxEntity: InboxEntity) :
    AndroidViewModel(application) {
    private var compositeDisposable = CompositeDisposable()
    lateinit var recordFile: File
    private var lastChatTime: Date? = null
    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val chatObservableLiveData: MutableLiveData<List<ChatModel>> = MutableLiveData()
    val refreshViewLiveData: MutableLiveData<ChatModel> = MutableLiveData()
    private var lastMessageTime: Date? = null
    private var broadCastForNetwork = CheckConnectivity()

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
        compositeDisposable.add(RxBus2.listen(DBInsertion::class.java).subscribeOn(Schedulers.computation()).subscribe {
            getUserRecentChats()
        })
        val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(broadCastForNetwork, filter)
    }

    inner class CheckConnectivity : BroadcastReceiver() {

        override fun onReceive(context: Context, arg1: Intent) {
            if (Utils.isInternetAvailable()) {
                SyncChatService.syncChatWithServer(refreshViewLiveData)
            }
        }
    }


    private fun getUserRecentChats() = viewModelScope.launch(Dispatchers.IO) {

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
            if (question != null) {
                when (question.material_type) {
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
                    AppObjectController.chatNetworkService.sendMessage(messageObject).await()
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
                    delay(500)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            if (Utils.isInternetAvailable()) {
                val arguments = mutableMapOf<String, String>()
                PrefManager.getLongValue(inboxEntity.conversation_id).let { time ->
                    if (time > 0) {
                        arguments["created"] = (time / 1000).toString()
                    }
                }
                NetworkRequestHelper.getUpdatedChat(
                    inboxEntity.conversation_id,
                    queryMap = arguments
                )
            }else{
                RxBus2.publish(MessageCompleteEventBus(false))

            }
        }
        refreshChatEverySomeTime()
    }


    private fun refreshChatEverySomeTime() {
        compositeDisposable.add(
            Observable.interval(1, 1, TimeUnit.MINUTES)
                .subscribeOn(Schedulers.computation())
                .timeInterval()
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    if (Utils.isInternetAvailable()) {
                        val arguments = mutableMapOf<String, String>()
                        PrefManager.getLongValue(inboxEntity.conversation_id).let { time ->
                            if (time > 0) {
                                arguments["created"] = (time / 1000).toString()
                            }
                        }
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
            if (readChatList.isNullOrEmpty().not()) {
                val idList = readChatList.map { it.chatId }.toMutableList()
                appDatabase.chatDao().updateMessageStatus(MESSAGE_STATUS.SEEN_BY_USER, idList)
            }
        }

    }
     fun refreshChatOnManual(){
        val arguments = mutableMapOf<String, String>()
        PrefManager.getLongValue(inboxEntity.conversation_id).let { time ->
            if (time > 0) {
                arguments["created"] = (time / 1000).toString()
            }
        }
        NetworkRequestHelper.getUpdatedChat(
            inboxEntity.conversation_id,
            queryMap = arguments
        )
    }

    /*private fun deleteMessageAndMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = mapOf("is_deleted" to "true")

            val listIds: MutableList<String> = mutableListOf()
            val chatReturn: List<ChatModel> = appDatabase.chatDao().getUnsyncDeletesMessage()
            if (chatReturn.isNotEmpty()) {
                chatReturn.forEach { chatModel ->
                    try {
                        chatModel.downloadedLocalPath?.let {
                            File(it).deleteOnExit()
                        }
                    } catch (ex: Exception) {

                    }

                    try {
                        AppObjectController.chatNetworkService.deleteMessage(
                            chatModel.chatId,
                            data
                        )
                        listIds.add(chatModel.chatId)
                    } catch (ex: Exception) {

                    }
                }
                appDatabase.chatDao().deleteUserMessages(listIds)
            }
        }
    }*/
}
