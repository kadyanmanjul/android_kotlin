package com.joshtalks.badebhaiya.feed

import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.datastore.BbDatastore
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.*
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.impressions.Records
import com.joshtalks.badebhaiya.liveroom.*
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubEventsManager
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.recordedRoomPlayer.di.ProcessRoomRecordingService
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.utils.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import timber.log.Timber
import java.io.File
import kotlinx.coroutines.async
import java.net.SocketTimeoutException

const val ROOM_ITEM = "room_item"
const val USER_ID = "user_id"
const val ROOM_DETAILS = "room_details"
const val TOPIC = "topic"

class FeedViewModel : ViewModel() {

    var source: String = EMPTY
    val isRoomsAvailable = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    var userID: String = ""
    var isRoomActive = MutableLiveData(false)
    var isRoomCreated = MutableLiveData(false)
    var isRoomsheduled = MutableLiveData(false)
    lateinit var respBody: ConversationRoomResponse
    val waitResponse = MutableLiveData<List<Waiting>>()
    var pubChannelName: String? = null
    var isSpeaker=MutableLiveData(false)
    lateinit var response: Response<ConversationRoomResponse>
    lateinit var roomtopic: String
    var isBackPressed = MutableLiveData(false)
    val searchResponse = MutableLiveData<SearchRoomsResponseList>()
    val feedAdapter = FeedAdapter()
    var message = Message()
    var profileUuid:String?=null
    lateinit var roomData: RoomListResponseItem
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    val repository = ConversationRoomRepository()
    val scheduleRoomStartDate = ObservableField<String>()
    val scheduleRoomStartTime = ObservableField<String>()
    val signUpRepository = BBRepository()
    var pubNubState = PubNubState.ENDED
    var isModerator = false
    val waitingRoomUsers = MutableLiveData<List<Waiting>>(emptyList())
    var speakerName = ""
    private var pendingRoomTopic = ""
    val previousRoomData = MutableLiveData<ConversationRoomResponse>()
    val previousRoomDataForSchedule = MutableLiveData<RoomListResponseItem>()
    val roomRequestCount = MutableLiveData<Int>()
    private var requestChannel: ListenerRegistration? = null
    private val jobs = mutableListOf<Job>()
    lateinit var currentRoom:ConversationRoomResponse
    val finishLiveRoom = MutableSharedFlow<Boolean>()
    val expandRecordedRoom = MutableLiveData<LiveRoomState>()



    private val commonRepository by lazy {
        CommonRepository()
    }

    var pendingRoomTopicForSchedule = ""
    var pendingRoomTimeForSchedule = ""

    init {

        collectRoomRequestCount()

    }

    val fansList: Flow<PagingData<Fans>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { BBRepository().fansPaginatedList(profileUuid!!) }
    )
        .flow

    val followingList: Flow<PagingData<Users>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { BBRepository().followingPaginatedList(profileUuid!!) }
    )
        .flow

    private fun collectPubNubState() {
        viewModelScope.launch {
            PubNubData.pubNubState.collect {
                pubNubState = it
            }
        }
    }

    private fun collectRoomRequestCount(){
        viewModelScope.launch {
            BbDatastore.roomRequestCount.collectLatest {
                roomRequestCount.postValue(it)
            }
        }
    }

    fun readRequestCount(){
        val db= FirebaseFirestore.getInstance()
        requestChannel=db.collection("PERSONAL_REQUEST_COUNT")
            .addSnapshotListener{ querySnapshot,firestoreException->
                firestoreException?.let {
                    showToast("Error")
                    return@addSnapshotListener
                }
                querySnapshot?.let {
                    viewModelScope.launch {
                        for(i in it.documents) {
                            if(i.id==User.getInstance().userId) {
                                BbDatastore.updateRoomRequestCount(i.data?.get("request_count") as Long)
                                return@launch
                            }
                            else BbDatastore.updateRoomRequestCount(0)
                        }
                    }
                }
            }
    }

    fun requestChannelEnd() {
        jobs.forEach {
            it.cancel()
        }
        jobs.clear()
        requestChannel?.remove()
        requestChannel?.remove()
    }

    fun getRoomRequestCount(){
            commonRepository.roomRequestCount()
    }

    private fun collectModeratorStatus() {
        viewModelScope.launch {
            // collect moderator joined status from firestore.

            WaitingRoomManager.hasSpeakerJoined.collect {

                isRoomActive.value = it
                roomData.speakersData?.userId?.let { it1 ->
                    joinRoom(
                        roomData.roomId.toString(), roomData.topic.toString(), "FEED_SCREEN",
                    )
                }
            }
        }
    }

    fun setIsBadeBhaiyaSpeaker() {
        isBadeBhaiyaSpeaker.set(User.getInstance().isSpeaker)
        isBadeBhaiyaSpeaker.notifyChange()
    }

    fun reader() {
        collectModeratorStatus()
//        PubNubManager.initSpeakerJoined()
    }

    fun createRoom(topic: String, callback: CreateRoom.CreateRoomCallback) {
        if (pubNubState == PubNubState.STARTED) {
            showToast("Please Leave Current Room")
            return
        }
        viewModelScope.launch {

            if (topic.isNullOrBlank()) {
                showToast(AppObjectController.joshApplication.getString(R.string.enter_topic_name))
            } else {
                try {
                    isRoomCreated.value = true
                    sendEvent(Impression("FEED_SCREEN", "CLICKED_CREATE"))
                    isLoading.set(true)
                    pendingRoomTopic = topic
                    val response = repository.createRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            topic = topic
                        )
                    )
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            currentRoom=response.body()!!
                            if (response.code() == 200) {
                                showToast("Room created successfully")
                                callback?.onRoomCreated(response.body()!!, topic)
                            } else if (response.code() == 202) {
                                isRoomCreated.value = false
                                callback?.onError("You Already have a Room")
                                previousRoomData.postValue(response.body())
                            }
                        } else
                            showToast("Oops Something Went Wrong! Try Again")
                    } else {
                        callback?.onError("An error occurred!")
                    }
                } catch (e: Exception) {
                    callback?.onError(e.localizedMessage)
                    e.showAppropriateMsg()
                } finally {
                    isLoading.set(false)
                }
                isRoomCreated.value = false
            }
        }
    }

    fun uploadCompressedMedia(mediaPath: File) {
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            ProcessRoomRecordingService.processSingleRoomRecording(context = Utils.context, videoPath = mediaPath.absolutePath?:"", audioPath = mediaPath.absolutePath, callId = currentRoom.roomId.toString(),agoraMentorId = currentRoom.roomId.toString(),recordDuration = 12345678.toInt())
        }
    }

    fun joinRoom(roomId: String, topic: String, source: String, isRejoin: Boolean = false) {
        Timber.d("JOIN ROOM PARAMS => room: $roomId and Topic => $topic")
//        pubChannelName = moderatorId
        if (pubNubState == PubNubState.STARTED) {
            if (roomId == PubNubManager.getLiveRoomProperties().roomId.toString()) {
//                showToast("Room Already Active")
                message.what = ROOM_EXPAND
                singleLiveEvent.value = message
            } else
                showToast("Please Leave Current Room")
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                isLoading.set(true)
                Log.d("YASH", "joinRoom: $source")
                response = repository.joinRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = roomId.toInt(),
                        fromPage = source,
                        is_rejoin = isRejoin
                    )
                )
                roomtopic = topic
                if (response.isSuccessful) {
                    if (response.body() != null) {
                        currentRoom=response.body()!!
//                        if (moderatorId == User.getInstance().userId) {
//                            isModerator = true
//                            PubNubEventsManager.sendModeratorStatus(true, moderatorId.toString())
//                        }
                        showToast("Room joined successfully")
                        message.what = OPEN_ROOM
                        message.data = Bundle().apply {
                            putParcelable(
                                ROOM_DETAILS,
                                response.body()
                            )
                            putString(
                                TOPIC,
                                topic
                            )
                        }
                        singleLiveEvent.value = message
                    } else
                        showToast("Oops Something Went Wrong! Try Again")

                } else {
                    if (response.code() == 406) {
                        PubNubManager.setStartingRoomProperties(
                            StartingLiveRoomProperties(
                                roomId = roomData.roomId
                            )
                        )
                        message.what = OPEN_WAIT_ROOM
                        singleLiveEvent.value = message
                    }
                    showToast(response.errorMessage())

                    }
                Log.d("sahil", "joinRoom:$response")

            } catch (e: SocketTimeoutException) {
                showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
            } catch (e: Exception) {
                Timber.d("JOIN ROOM ERROR => ${e.stackTrace}")
                Timber.d("JOIN ROOM ERROR => ${e.message}")
                e.printStackTrace()
                e.showAppropriateMsg()
            } finally {
                isLoading.set(false)
            }
        }
    }

    fun getWaitingList(): List<Waiting>? {
        viewModelScope.launch {
            try {
                val resp = repository.waitingList()
                if (resp.isSuccessful) {
                    waitResponse.value = resp.body()!!.users
                }
            } catch (Ex: Exception) {
            }
        }
        return waitResponse.value
    }

    fun getRooms(list: MutableList<RoomListResponseItem>) {

        viewModelScope.launch {
            try {
                isLoading.set(true)
                // isRoomsAvailable.set(true)
                sendEvent(Impression("FEED_SCREEN", "REFRESH_CALLED"))
                val res = repository.getRoomList()
                if (res.isSuccessful) {
                    res.body()?.let {
                        isSpeaker.value=it.isSpeaker
                        User.getInstance().isSpeaker=it.isSpeaker!!
                        if (it.scheduledRoomList.isNullOrEmpty().not())
                            list.addAll(it.scheduledRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    if (roomListResponseItem.isScheduled == true)
                                        ConversationRoomType.SCHEDULED
                                    else
                                        ConversationRoomType.NOT_SCHEDULED
                                roomListResponseItem
                            })
                        if (it.liveRoomList.isNullOrEmpty().not())
                            list.addAll(it.liveRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    ConversationRoomType.LIVE
                                roomListResponseItem
                            })

                        if (list.isNullOrEmpty())
                            isRoomsAvailable.set(false)
                        else {
                            isRoomsAvailable.set(true)
                            list.forEach { listItem ->
                                listItem.currentTime = it.currentTime
                            }
                            feedAdapter.submitList(list.toList().reversed())
                        }
                    }

                } else {
                    isRoomsAvailable.set(false)
                    feedAdapter.submitList(list.toList().reversed())
                }
            } catch (ex: Exception) {
                feedAdapter.submitList(list.toList().reversed())
                isRoomsAvailable.set(false)
                ex.printStackTrace()
            } finally {
                isLoading.set(false)
                delay(500)
                message.what = SCROLL_TO_TOP
                singleLiveEvent.postValue(message)
            }

        }

    }

    fun getRecordRooms() {
        val list = mutableListOf<RoomListResponseItem>()

        viewModelScope.launch {
            try{
                val res=repository.getRecordsList()
                if(res.isSuccessful){
                    res.body()?.let {

//                        val recordList= mutableListOf<RoomListResponseItem>()
                        if(it.recordings.isNullOrEmpty().not())
                        {
                            list.addAll(it.recordings.map { recordListResponseItem ->
                                recordListResponseItem.conversationRoomType =
                                    ConversationRoomType.RECORDED
                                recordListResponseItem
                            })
                        }
                        if (list.isNullOrEmpty().not()) {
                            isRoomsAvailable.set(true)
//                            feedAdapter.submitList(list.reversed())
                        }

                    }
                }
            }catch(ex:Exception){

            }
            getRooms(list)

        }
    }


    fun searchUser(query: String) {
        val listUser = mutableListOf<Users>()
        viewModelScope.launch {
            try {
                val parems = mutableMapOf<String, String>()
                parems["query"] = query
                val response = repository.searchRoom(parems)
                if (response.isSuccessful) {
                    response.body()?.let {
                        searchResponse.postValue(it)
                    }
                }
            } catch (ex: Exception) {

            }
        }
    }

    fun setReminder(reminderRequest: ReminderRequest) {
        viewModelScope.launch {
            try {
                val res = repository.setReminder(reminderRequest)
                if (res.isSuccessful && res.code() == 201) {
                    showToast("Reminder Set")
                    feedAdapter.notifyDataSetChanged()
                } else showToast("Error while setting reminder")
            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Error while setting reminder")
            } finally {
            }
        }
    }

    fun sendEvent(source: Impression) {
        viewModelScope.launch {
            try {
                repository.sendEvent(source)
            } catch (e: Exception) {

            }
        }
    }

    fun userRoomRecord(id:Int, user:String){
        viewModelScope.launch {
            try {
                repository.userRoomRecord(id,user)
            }catch (ex:Exception){

            }
        }
    }

    fun scheduleRoom(topic: String, startTime: String, callback: CreateRoom.CreateRoomCallback) {
        viewModelScope.launch {
            if (topic.isNullOrBlank()) {
                showToast(AppObjectController.joshApplication.getString(R.string.enter_topic_name))
            } else {
                try {
                    isRoomsheduled.value=true
//                    if (Utils.getEpochTimeFromFullDate(startTime) <
//                        (System.currentTimeMillis() + IST_TIME_DIFFERENCE + ALLOWED_SCHEDULED_TIME)) {
//                        showToast("Schedule a room for 30 minutes or later!")
//                        return@launch
//                    }
                    sendEvent(Impression("FEED_SCREEN", "CLICKED_SCHEDULE"))
                    isLoading.set(true)
                    val response = repository.scheduleRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            topic = topic,
                            startTime = startTime
                        )
                    )

                    pendingRoomTimeForSchedule = startTime
                    pendingRoomTopicForSchedule = topic
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (response.code() == 200){
                                showToast("Room scheduled successfully")
                                feedAdapter.addScheduleRoom(it)
                                callback.onRoomSchedule(it)
                            } else if (response.code() == 202){
                                callback.onError("You already have a room")
                                previousRoomDataForSchedule.postValue(response.body())
                            }
                        }
                    } else callback.onError("An error occurred!")
                } catch (e: Exception) {
                    callback.onError(e.localizedMessage)
                    showToast("Error while creating room")
                } finally {
                    isLoading.set(false)
                }
                isRoomsheduled.value=false
            }
        }
    }

    fun setScheduleStartDate(date: String) {
        scheduleRoomStartDate.set(date)
    }

    fun setScheduleStartTime(time: String) {
        scheduleRoomStartTime.set(time)
    }

    fun endPreviousRoom(roomId: Int, callback: CreateRoom.CreateRoomCallback) {
        viewModelScope.launch {
            try {
                val response = repository.endRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = roomId
                    )
                )

                if (response.isSuccessful){
                    delay(1000)
                    createRoom(pendingRoomTopic, callback)
                } else {
                    showToast("Something went wrong")
                }
            } catch (e: Exception){
                e.showAppropriateMsg()
            }
        }
    }

    fun endPreviousRoomAndSchedule(previousRoomId: Int, callback: CreateRoom.CreateRoomCallback){
        viewModelScope.launch {
            try {
                val response = repository.endRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = previousRoomId
                    )
                )

                if (response.isSuccessful){
                    scheduleRoom(pendingRoomTopicForSchedule, pendingRoomTimeForSchedule, callback)
                } else {
                    showToast("Something went wrong")
                }
            } catch (e: Exception){
                e.showAppropriateMsg()
            }
        }
    }

    fun joinPreviousRoom() {
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    isLoading.set(true)
                    Log.d("YASH", "joinRoom:")
                    response = repository.joinRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            roomId = previousRoomData.value?.roomId,
                            fromPage = "FEED_SCREEN"
                        )
                    )
                    roomtopic = previousRoomData.value?.roomName ?: ""
                    if (response.isSuccessful) {
                        if (response.body() != null) {
//                            if (moderatorId == User.getInstance().userId) {
                                isModerator = true
                                PubNubEventsManager.sendModeratorStatus(true, User.getInstance().userId)
//                            }
                            showToast("Room joined successfully")
                            message.what = OPEN_ROOM
                            message.data = Bundle().apply {
                                putParcelable(
                                    ROOM_DETAILS,
                                    response.body()
                                )
                                putString(
                                    TOPIC,
                                    roomtopic
                                )
                            }
                            singleLiveEvent.value = message
                        } else
                            showToast("Oops Something Went Wrong! Try Again")

                    } else {
                        if (response.code() == 406) {
                            PubNubManager.setStartingRoomProperties(
                                StartingLiveRoomProperties(
                                    roomId = roomData.roomId
                                )
                            )
                            message.what = OPEN_WAIT_ROOM
                            singleLiveEvent.value = message
                        }
                        showToast(response.errorMessage())

                        }
                    Log.d("sahil", "joinRoom:$response")

                } catch (e: SocketTimeoutException) {
                    showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                } catch (e: Exception) {
                    Timber.d("JOIN ROOM ERROR => ${e.stackTrace}")
                    Timber.d("JOIN ROOM ERROR => ${e.message}")
                    e.printStackTrace()
                    e.showAppropriateMsg()
                } finally {
                    isLoading.set(false)
                }
            }
        }
    }


interface Call {
    fun itemClick(userId: String)
}
