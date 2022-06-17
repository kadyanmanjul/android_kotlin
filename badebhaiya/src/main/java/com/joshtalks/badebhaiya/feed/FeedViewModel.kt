package com.joshtalks.badebhaiya.feed

import android.graphics.Insets.add
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.collection.ArraySet
import androidx.compose.runtime.mutableStateListOf
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.*
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.*
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubEventsManager
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import timber.log.Timber
const val ROOM_ITEM = "room_item"
const val USER_ID = "user_id"
const val ROOM_DETAILS = "room_details"
const val TOPIC = "topic"

class FeedViewModel : ViewModel() {

    var source:String= EMPTY
    val isRoomsAvailable = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    var userID:String=""
    var isRoomActive=MutableLiveData(false)
    lateinit var respBody: ConversationRoomResponse
    val waitResponse= MutableLiveData<List<Waiting>>()
    var pubChannelName:String?=null
    lateinit var response: Response<ConversationRoomResponse>
    lateinit var roomtopic:String
    var isBackPressed=MutableLiveData(false)
    val searchResponse=MutableLiveData<SearchRoomsResponseList>()
    val feedAdapter = FeedAdapter()
    var message = Message()
    lateinit var roomData:RoomListResponseItem
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    val repository = ConversationRoomRepository()
    val scheduleRoomStartDate = ObservableField<String>()
    val scheduleRoomStartTime = ObservableField<String>()
    val signUpRepository = BBRepository()
    var pubNubState = PubNubState.ENDED
    var modStat=Message()
    var isModerator=false
    val waitingRoomUsers = MutableLiveData<List<Waiting>>(emptyList())
    var speakerName = ""
    init {
        collectPubNubState()

//            viewModelScope.launch {
//                try{
//                    val response = signUpRepository.getBBtoFollowList(1)
//                    Timber.d("response => ${response.body()}")
//
//                } catch (e: Exception){
//                    e.printStackTrace()
//                }
//            }

    }

    private fun collectPubNubState() {
        viewModelScope.launch {
            PubNubData.pubNubState.collect {
                pubNubState = it
            }
        }
    }

    private fun collectModeratorStatus(){
        viewModelScope.launch {
            PubNubData.status.collect{
                modStat=it
                Log.i("MODERATORSTATUS", "collectModeratorStatus launch: ${PubNubData.status}")
                Log.i("MODERATORSTATUS", "collectModeratorStatus: $it")
//                message.what = OPEN_ROOM
//                message.data = Bundle().apply {
//                    putParcelable(
//                        ROOM_DETAILS,
//                        respBody
//                    )
//                    putString(
//                        TOPIC,
//                        roomtopic
//                    )
//                }
                isRoomActive.value=true
                roomData.speakersData?.userId?.let { it1 ->
                    joinRoom(roomData.roomId.toString(),roomData.topic.toString(),"FEED_SCREEN",
                        it1
                    )
                }
                PubNubManager.waitingUnsubscribe()
                Log.i("YASHEN", "postvalue: ")
//                singleLiveEvent.value=message
            }
        }
    }

    fun setIsBadeBhaiyaSpeaker() {
        isBadeBhaiyaSpeaker.set(User.getInstance().isSpeaker)
        isBadeBhaiyaSpeaker.notifyChange()
    }

    fun reader(){
        Log.i("MODERATORSTATUS", "reader: ")
        collectModeratorStatus()
        PubNubManager.initSpeakerJoined()
    }

    fun createRoom(topic: String, callback: CreateRoom.CreateRoomCallback) {
        if (pubNubState == PubNubState.STARTED){
            showToast("Please Leave Current Room")
            return
        }
        viewModelScope.launch {
            if (topic.isNullOrBlank()) {
                showToast(AppObjectController.joshApplication.getString(R.string.enter_topic_name))
            } else {
                try {
                    isLoading.set(true)
                    val response = repository.createRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            topic = topic
                        )
                    )
                    if (response.isSuccessful) {
                        showToast("Room created successfully")
                        callback.onRoomCreated(response.body()!!, topic)
                    } else callback.onError("An error occurred!")
                } catch (e: Exception) {
                    callback.onError(e.localizedMessage)
                    e.showAppropriateMsg()
                } finally {
                    isLoading.set(false)
                }
            }
        }
    }

    fun joinRoom(roomId: String, topic: String = "sahil", source: String, moderatorId: String?) {
        Timber.d("JOIN ROOM PARAMS => room: $roomId and Topic => $topic")
        pubChannelName = moderatorId
        if (pubNubState == PubNubState.STARTED){
            if(roomId==PubNubManager.getLiveRoomProperties().roomId.toString()) {
//                showToast("Room Already Active")
                message.what = ROOM_EXPAND
                singleLiveEvent.value = message
            }
            else
            showToast("Please Leave Current Room")
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                isLoading.set(true)
                Log.d("YASH", "joinRoom:")
                 response = repository.joinRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = roomId.toInt(),
                        fromPage = source
                    )
                )
                roomtopic=topic
                if (response.isSuccessful) {
                        if (moderatorId == User.getInstance().userId) {
                            isModerator=true
                            PubNubEventsManager.sendModeratorStatus(true, moderatorId.toString())
                        }
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
                        Log.i("YASHEN", "postvalue: ")
                        singleLiveEvent.value = message

                }
                else
                {
                    if(response.code()==406){

                        message.what = OPEN_WAIT_ROOM
                        singleLiveEvent.value = message
                    }

                    Log.i("YASHEN", "joinRoom: failed")
                }
                Log.d("sahil", "joinRoom:$response")

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

    fun getWaitingList() : List<Waiting>? {
        viewModelScope.launch {
            try {
                val resp=repository.waitingList()
                if(resp.isSuccessful)
                {
                    waitResponse.value = resp.body()!!.users
                }
            }catch (Ex:Exception){
            }
        }
        return waitResponse.value
    }

    fun getRooms() {
        viewModelScope.launch {
            try {
                isLoading.set(true)
               // isRoomsAvailable.set(true)
                ProfileViewModel().sendEvent(Impression("FEED_SCREEN","REFRESH_CALLED"))
                val res = repository.getRoomList()
                if (res.isSuccessful) {
                    res.body()?.let {
                        val list = mutableListOf<RoomListResponseItem>()
                        if (it.liveRoomList.isNullOrEmpty().not())
                            list.addAll(it.liveRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    ConversationRoomType.LIVE
                                roomListResponseItem
                            })
                        if (it.scheduledRoomList.isNullOrEmpty().not())
                            list.addAll(it.scheduledRoomList!!.map { roomListResponseItem ->
                                roomListResponseItem.conversationRoomType =
                                    if (roomListResponseItem.isScheduled == true)
                                        ConversationRoomType.SCHEDULED
                                    else
                                        ConversationRoomType.NOT_SCHEDULED
                                roomListResponseItem
                            })
                        if (list.isNullOrEmpty())
                            isRoomsAvailable.set(false)
                        else {
                            isRoomsAvailable.set(true)
                            list.forEach { listItem ->
                                listItem.currentTime = it.currentTime
                            }
                            feedAdapter.submitList(list.toList())
                        }
                    }
                    message.what= SCROLL_TO_TOP
                    singleLiveEvent.postValue(message)

                } else {
                    isRoomsAvailable.set(false)
                    feedAdapter.submitList(emptyList())
                }
            } catch (ex: Exception) {
                feedAdapter.submitList(emptyList())
                isRoomsAvailable.set(false)
                ex.printStackTrace()
            } finally {
                isLoading.set(false)
            }
        }
    }


    fun searchUser(query: String) {
        val listUser= mutableListOf<Users>()
        viewModelScope.launch {
            try {
                val parems = mutableMapOf<String, String>()
                parems["query"] = query
                val response = repository.searchRoom(parems)
                if(response.isSuccessful)
                {
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

    fun scheduleRoom(topic: String, startTime: String, callback: CreateRoom.CreateRoomCallback) {
        viewModelScope.launch {
            if (topic.isNullOrBlank()) {
                showToast(AppObjectController.joshApplication.getString(R.string.enter_topic_name))
            } else {
                try {
//                    if (Utils.getEpochTimeFromFullDate(startTime) <
//                        (System.currentTimeMillis() + IST_TIME_DIFFERENCE + ALLOWED_SCHEDULED_TIME)) {
//                        showToast("Schedule a room for 30 minutes or later!")
//                        return@launch
//                    }
                    isLoading.set(true)
                    val response = repository.scheduleRoom(
                        ConversationRoomRequest(
                            userId = User.getInstance().userId,
                            topic = topic,
                            startTime = startTime
                        )
                    )
                    if (response.isSuccessful) {
                        response.body()?.let {
                            showToast("Room scheduled successfully")
                            feedAdapter.addScheduleRoom(it)
                            callback.onRoomSchedule(it)
                        }
                    } else callback.onError("An error occurred!")
                } catch (e: Exception) {
                    callback.onError(e.localizedMessage)
                    showToast("Error while creating room")
                } finally {
                    isLoading.set(false)
                }
            }
        }
    }

    fun setScheduleStartDate(date: String) {
        scheduleRoomStartDate.set(date)
    }

    fun setScheduleStartTime(time: String) {
        scheduleRoomStartTime.set(time)
    }
}

interface Call
{
    fun itemClick(userId:String)
}
