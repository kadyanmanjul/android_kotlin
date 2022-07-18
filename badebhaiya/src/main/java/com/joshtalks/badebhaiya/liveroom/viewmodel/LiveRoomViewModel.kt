package com.joshtalks.badebhaiya.liveroom.viewmodel

import android.app.Application
import android.os.Message
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.showAppropriateMsg
import com.joshtalks.badebhaiya.datastore.BbDatastore
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.LiveRoomState
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomListingNavigation
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

const val NOTIFICATION_ID = "notification_id"
const val NOTIFICATION_BOOLEAN = "notification_boolean"
const val NOTIFICATION_NAME = "notification_name"
const val NOTIFICATION_TYPE = "notification_type"
const val NOTIFICATION_USER = "notification_type"

class LiveRoomViewModel(application: Application) : AndroidViewModel(application) {

    val navigation = MutableLiveData<ConversationRoomListingNavigation>()

    //val roomListLiveData = MutableLiveData<RoomListResponse>()
    var audienceList = MutableLiveData<List<LiveRoomUser>>(listOf())
    var speakersList = MutableLiveData<List<LiveRoomUser>>(listOf())
    val pubNubState = MutableLiveData<PubNubState>()
    val deflate = MutableLiveData(false)
    val liveRoomState = MutableLiveData<LiveRoomState>()
    var lvRoomState: LiveRoomState? = null
    private val jobs = arrayListOf<Job>()
    var message = Message()
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    private val repository by lazy {
        ConversationRoomRepository()
    }
    private val commonRepository by lazy {
        CommonRepository()
    }
    val roomRequestCount = MutableLiveData<Int>()


    fun getSpeakerList() = this.speakersList.value ?: emptyList<LiveRoomUser>()

    fun getAudienceList() = this.audienceList.value ?: emptyList<LiveRoomUser>()

    fun getRaisedHandAudienceSize(): Int =
        this.audienceList.value?.filter { it.isSpeaker == false && it.isHandRaised && it.isSpeakerAccepted.not() }?.size
            ?: 0

    fun joinRoom(item: RoomListResponseItem) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {

                val joinRoomRequest =
                    ConversationRoomRequest(
                        userId = User.getInstance().userId,
                        roomId = item.roomId
                    )
                val apiResponse = repository.joinRoom(joinRoomRequest)
                if (apiResponse.isSuccessful) {
                    val response = apiResponse.body()
                    navigation.postValue(
                        ConversationRoomListingNavigation.OpenConversationLiveRoom(
                            response?.channelName,
                            response?.uid,
                            response?.token,
                            item.startedBy ?: 0 == response?.uid,
                            response?.roomId,
                            startedBy = item.startedBy,
                            topic = item.topic ?: EMPTY
                        )
                    )

                } else {
                    val errorResponse = Gson().fromJson(
                        apiResponse.errorBody()?.string(),
                        ConversationRoomResponse::class.java
                    )
                    navigation.postValue(
                        ConversationRoomListingNavigation.ApiCallError(
                            " Error occured"
                        )
                    )

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    init {
        collectRoomRequestCount()
    }

    fun startRoom() {
        collectData()
        PubNubManager.initPubNub()
    }

    private fun collectData() {
        Log.d("sahil", "collectData called: ")
        viewModelScope.launch {
            PubNubData.audienceList
//                .map { it.reversed().distinctBy { it.userId }.reversed()}
                .map { it.sortedBy { it.sortOrder } }
                .collect {
                    Log.d("sahil", "audience list => $it")

                    audienceList.postValue(it)
                }
        }

        viewModelScope.launch {
            PubNubData.speakerList
//                .map { it.reversed().distinctBy {  it.userId }.reversed() }
                .map { it.sortedByDescending { it.sortOrder } }
                .collect {
                    Log.d("sahil", "speakers list =>$it")
                    speakersList.postValue(it.toList())
                }
        }
        viewModelScope.launch {
            PubNubData.liveEvent.collect {
                singleLiveEvent.postValue(it)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            PubNubData.pubNubState.collect {
                pubNubState.postValue(it)
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

    fun getRoomRequestCount() {
        commonRepository.roomRequestCount()
    }

    fun setChannelMemberStateForUuid(
        user: LiveRoomUser?,
        isMicOn: Boolean? = null,
        channelName: String?
    ) {
        PubNubManager.setChannelMemberStateForUuid(user, isMicOn, channelName)
    }

    fun unSubscribePubNub() {
        PubNubManager.unSubscribePubNub()
        flushLiveData()
    }

    fun flushLiveData() {
        speakersList = MutableLiveData<List<LiveRoomUser>>(listOf())
        audienceList = MutableLiveData<List<LiveRoomUser>>(listOf())
        singleLiveEvent = MutableLiveData()
    }

    fun reconnectPubNub() {
        PubNubManager.reconnectPubNub()
    }

}
