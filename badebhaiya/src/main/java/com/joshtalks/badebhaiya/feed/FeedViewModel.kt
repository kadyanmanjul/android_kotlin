package com.joshtalks.badebhaiya.feed

import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.showAppropriateMsg
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.*
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.SCROLL_TO_TOP
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.ALLOWED_SCHEDULED_TIME
import com.joshtalks.badebhaiya.utils.IST_TIME_DIFFERENCE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

const val ROOM_ITEM = "room_item"
const val USER_ID = "user_id"
const val ROOM_DETAILS = "room_details"
const val TOPIC = "topic"

class FeedViewModel : ViewModel() {

    val isRoomsAvailable = ObservableBoolean(true)
    val isLoading = ObservableBoolean(false)
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    var userID:String=""
    val searchResponse=MutableLiveData<SearchRoomsResponseList>()
    val feedAdapter = FeedAdapter()
    var message = Message()
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    val repository = ConversationRoomRepository()
    val scheduleRoomStartDate = ObservableField<String>()
    val scheduleRoomStartTime = ObservableField<String>()
    var pubNubState = PubNubState.ENDED

    init {
        collectPubNubState()
    }

    private fun collectPubNubState() {
        viewModelScope.launch {
            PubNubData.pubNubState.collect {
                pubNubState = it
            }
        }
    }

    fun setIsBadeBhaiyaSpeaker() {
        isBadeBhaiyaSpeaker.set(User.getInstance().isSpeaker)
        isBadeBhaiyaSpeaker.notifyChange()
    }

    /*val onFeedItemClicked: (RoomListResponseItem?, View?) -> Unit = { item, view ->
        message.what = OPEN_ROOM
        message.data = Bundle().apply {
            putParcelable(
                ROOM_ITEM,
                item
            )
        }
        singleLiveEvent.postValue(message)
    }*/

    fun onProfileClicked() {
        message.what = OPEN_PROFILE
        message.data = Bundle().apply {
            putString(
                USER_ID,
                User.getInstance().userId
            )
        }
        singleLiveEvent.postValue(message)
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

    fun joinRoom(item: RoomListResponseItem) {
        if (pubNubState == PubNubState.STARTED){
            showToast("Please Leave Current Room")
            return
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                isLoading.set(true)
                val response = repository.joinRoom(
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = item.roomId
                    )
                )
                if (response.isSuccessful) {
                    showToast("Room joined successfully")
                    message.what = OPEN_ROOM
                    message.data = Bundle().apply {
                        putParcelable(
                            ROOM_DETAILS,
                            response.body()
                        )
                        putString(
                            TOPIC,
                            item.topic
                        )
                    }
                    Log.i("YASHEN", "postvalue: ")
                    singleLiveEvent.value=message
                }
                Log.d("sahil", "joinRoom:$response")

            } catch (e: Exception) {
                e.showAppropriateMsg()
            } finally {
                isLoading.set(false)
            }
        }
    }

    fun getRooms() {
        viewModelScope.launch {
            try {
                isLoading.set(true)
                isRoomsAvailable.set(true)
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


    fun setTimer(){

    }

    fun searchRoom(query: String) {
        viewModelScope.launch {
            try {
                val parems = mutableMapOf<String, String>()
                parems["query"] = query
                val response = repository.searchRoom(parems)
                //if (response.isSuccessful)
                    //showToast("Search API launched successfully")
            } catch (ex: Exception) {

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

    fun deleteReminder(deleteReminderRequest: DeleteReminderRequest) {
        viewModelScope.launch {
            try {
                val res = repository.deleteReminder(deleteReminderRequest)
                if (res.isSuccessful && res.code() == 200) {
                    //showToast("Reminder Deleted")
                    feedAdapter.notifyDataSetChanged()
                } else showToast("Error while Deleting Reminder")
            } catch (ex: Exception) {
                ex.printStackTrace()
                showToast("Error while Deleting Reminder")
            } finally {
            }
        }
    }

    fun setReminder(reminderRequest: ReminderRequest) {
        viewModelScope.launch {
            try {
                val res = repository.setReminder(reminderRequest)
                if (res.isSuccessful && res.code() == 201) {
                    //showToast("Reminder set successfully")
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
                            callback.onRoomSchedule()
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
