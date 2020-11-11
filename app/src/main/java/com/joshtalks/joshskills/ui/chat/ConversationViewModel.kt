package com.joshtalks.joshskills.ui.chat

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.recorder.AudioRecording
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.chat_message.BaseChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import com.joshtalks.joshskills.repository.server.groupchat.GroupDetails
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.repository.service.SyncChatService
import id.zelory.compressor.Compressor
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.util.ConcurrentModificationException
import java.util.Date
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
    val emptyChatLiveData: MutableLiveData<Nothing> = MutableLiveData()
    val refreshViewLiveData: MutableLiveData<ChatModel> = MutableLiveData()
    private var lastMessageTime: Date? = null
    private var broadCastForNetwork = CheckConnectivity()
    private var mRefreshControl = true
    private val mAudioRecording: AudioRecording = AudioRecording()
    private var isRecordingStarted = false
    private val jobs = arrayListOf<Job>()
    val userLoginLiveData: MutableLiveData<GroupDetails> = MutableLiveData()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()

    init {
        addObserver()
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
        AppDirectory.tempRecordingFile().let {
            mAudioRecording.setOnAudioRecordListener(onRecordListener)
            mAudioRecording.setFile(it.absolutePath)
            recordFile = it
            mAudioRecording.startRecording()
            return@let true
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

    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(DBInsertion::class.java).subscribeOn(Schedulers.io()).subscribe {
                jobs += getUserRecentChats()
            })
        val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        context.registerReceiver(
            broadCastForNetwork,
            filter
        )
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
        listOfChat.forEach { chat ->
            val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
            question?.run {

                question.lesson = appDatabase.lessonDao().getLesson(question.lesson_id)

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
                if (this.parent_id.isNullOrEmpty().not()) {
                    chat.parentQuestionObject =
                        appDatabase.chatDao().getQuestionOnId(this.parent_id!!)
                }
                if (assessmentId != null) {
                    question.vAssessmentCount = AppObjectController.appDatabase.assessmentDao()
                        .countOfAssessment(assessmentId)
                }
                chat.question = question
            }

            if (chat.type == BASE_MESSAGE_TYPE.Q && question == null) {
                return@forEach
            }

            checkLesson(chatReturn, chat)
        }
        if (chatReturn.isNullOrEmpty()) {
            if (lastMessageTime != null) {
                emptyChatLiveData.postValue(null)
            }
            return@launch
        }
        lastMessageTime = chatReturn.last().created
        chatObservableLiveData.postValue(chatReturn)
        updateAllMessageReadByUser()
    }

    private fun checkLesson(chatList: MutableList<ChatModel>, chat: ChatModel) {
        val lessonModel = chat.question?.lesson
        if (lessonModel != null) {
            //It means This chat is a part of some lesson.

            if (chatList.isEmpty()) {
                addNewLesson(lessonModel, chatList, chat)
            } else {
                val lastChatModelInList = chatList.last()
                if (lastChatModelInList.type != BASE_MESSAGE_TYPE.LESSON) {
                    //Check if last chat in the current list is a lesson
                    //if its not then we create a new chat object with same data as current chat obejct but chane type to Lesson and add it to list

                    addNewLesson(lessonModel, chatList, chat)
                } else {

                    if (lastChatModelInList.lessonId != lessonModel.id) {
                        //checking wheather last chat and current chat belong to same lesson. No
                        addNewLesson(lessonModel, chatList, chat)
                    } else {
                        // it means last chat and current chat belong to same lesson. add current chat to last lesson
//                        lastChatModelInList.lessons?.add(chat)
//                        lastChatModelInList.lessonStatus = lessonModel.status //chat.question.status

//                        chatList.set(chatList.size - 1, lastChatModelInList)
                    }
                }
            }
        } else {
            //current chat object is not part of any lesson we will directly add it to the list
            chatList.add(chat)
        }

    }

    fun addNewLesson(
        lessonModel: LessonModel,
        chatList: MutableList<ChatModel>,
        chat: ChatModel
    ) {
//        val lessonChat = ChatModel()
        /*lessonChat.created = chat.created
        lessonChat.chatId = chat.chatId
        lessonChat.conversationId = chat.conversationId
        lessonChat.isSync = chat.isSync
        lessonChat.lastUseTime = chat.lastUseTime
        lessonChat.lessonId = lessonModel.id*/
        chat.type = BASE_MESSAGE_TYPE.LESSON
//        lessonModel.status = lessonChat.question?.lessonStatus

        chat.lessons = lessonModel

        chatList.add(chat)
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
            jobs += getUserRecentChats()
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
        getUserUnlockClass(date)
    }

    private fun getUserUnlockClass(date: Date) = viewModelScope.launch(Dispatchers.IO) {

        val chatReturn: MutableList<ChatModel> = mutableListOf()

        val listOfChat: List<ChatModel> =
            appDatabase.chatDao().getRecentChatAfterTime(inboxEntity.conversation_id, date)
        if (listOfChat.isNotEmpty()) {
            lastChatTime = listOfChat.last().created
        }
        listOfChat.forEach { chat ->
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
                return@forEach
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
        jobs += viewModelScope.launch(Dispatchers.IO) {
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
        jobs += NetworkRequestHelper.getUpdatedChat(
            inboxEntity.conversation_id,
            queryMap = arguments
        )
    }

    fun updateBatchChangeRequest() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.changeBatchRequest(inboxEntity.conversation_id)
                if (response.isSuccessful) {
                    deleteChatModelOfType(BASE_MESSAGE_TYPE.UNLOCK)
                    refreshChatOnManual()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    fun deleteChatModelOfType(type: BASE_MESSAGE_TYPE) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.chatDao()
                .deleteSpecificTypeChatModel(inboxEntity.conversation_id, type)
        }
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

    fun refreshChat() {
        lastChatTime = null
        getAllUserMessage()
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        context.unregisterReceiver(broadCastForNetwork)
        if (isRecordingStarted) {
            mAudioRecording.stopRecording(true)
        }
        try {
            val iterator = jobs.listIterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                item.cancel()
                iterator.remove()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    fun initCometChat() {
        jobs += viewModelScope.launch(Dispatchers.IO) {

            isLoading.postValue(true)

            val appSettings = AppSettings.AppSettingsBuilder()
                .subscribePresenceForAllUsers()
                .setRegion(BuildConfig.COMETCHAT_REGION)
                .build()

            CometChat.init(
                AppObjectController.joshApplication,
                BuildConfig.COMETCHAT_APP_ID,
                appSettings,
                object : CometChat.CallbackListener<String>() {
                    override fun onSuccess(p0: String?) {
                        Timber.d("Initialization completed successfully")
                        getGroupDetails(inboxEntity.conversation_id)
                    }

                    override fun onError(p0: CometChatException?) {
                        Timber.d("Initialization failed with exception: %s", p0?.message)
                        isLoading.postValue(false)
                        showToast(
                            context.getString(R.string.generic_message_for_error),
                            Toast.LENGTH_SHORT
                        )
                    }

                })

        }
    }

    private fun getGroupDetails(conversationId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = mapOf(Pair("conversation_id", conversationId))
                val response =
                    AppObjectController.chatNetworkService.getGroupDetails(params)

                loginUser(response)

            } catch (ex: Exception) {
                ex.printStackTrace()
                isLoading.postValue(false)
                showToast(context.getString(R.string.generic_message_for_error), Toast.LENGTH_SHORT)
            }
        }
    }

    private fun loginUser(groupDetails: GroupDetails) {

        if (CometChat.getLoggedInUser() == null) {
            // User not logged in
            CometChat.login(
                groupDetails.userId,
                BuildConfig.COMETCHAT_API_KEY,
                object : CometChat.CallbackListener<User>() {
                    override fun onSuccess(p0: User?) {
                        Timber.d("Login Successful : %s", p0?.toString())
                        isLoading.postValue(false)
                        userLoginLiveData.postValue(groupDetails)
                    }

                    override fun onError(p0: CometChatException?) {
                        Timber.d("Login failed with exception: %s", p0?.message)
                        isLoading.postValue(false)
                        showToast(
                            context.getString(R.string.generic_message_for_error),
                            Toast.LENGTH_SHORT
                        )
                    }

                })
        } else if (CometChat.getLoggedInUser().uid != groupDetails.userId) {
            // Any other user is logged in. So we have to logout first
            CometChat.logout(object : CometChat.CallbackListener<String>() {
                override fun onSuccess(p0: String?) {
                    loginUser(groupDetails)
                }

                override fun onError(p0: CometChatException?) {
                    Timber.d("Logout previous user failed with exception: %s", p0?.message)
                    isLoading.postValue(false)
                    showToast(
                        context.getString(R.string.generic_message_for_error),
                        Toast.LENGTH_SHORT
                    )
                }

            })
        } else {
            // User already logged in
            isLoading.postValue(false)
            userLoginLiveData.postValue(groupDetails)
        }
    }

}
