package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.joshtalks.joshskills.conversationRoom.model.*
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.ApiCallError
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.OpenConversationLiveRoom
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HAS_SEEN_CONVO_ROOM_POINTS
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ConversationRoomListingViewModel (application: Application) : AndroidViewModel(application) {
    val navigation = MutableLiveData<ConversationRoomListingNavigation>()
    val roomDetailsLivedata = MutableLiveData<ConversationRoomDetailsResponse>()
    val roomListLiveData = MutableLiveData<RoomListResponse>()
    val points = MutableLiveData<String>()
    val isRoomEnded = MutableLiveData<Boolean>(false)
    var audienceList = MutableLiveData<ArrayList<LiveRoomUser>>()
    private val jobs = arrayListOf<Job>()

    fun joinRoom(item: RoomListResponseItem) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                var qId :Int? = null
                if (item.conversationRoomQuestionId!=null && ( item.conversationRoomQuestionId!=0 || item.conversationRoomQuestionId != -1) ){
                    qId = item.conversationRoomQuestionId
                }
                val joinRoomRequest =
                    JoinConversionRoomRequest(Mentor.getInstance().getId(), item.roomId.toInt(),qId)

                val apiResponse =
                    AppObjectController.conversationRoomsNetworkService.joinConversationRoom(
                        joinRoomRequest
                    )
                if (apiResponse.isSuccessful) {
                    val response = apiResponse.body()
                    navigation.postValue(
                        OpenConversationLiveRoom(
                            response?.channelName,
                            response?.uid,
                            response?.token,
                            item.startedBy?:0 == response?.uid,
                            response?.roomId?.toInt() ?: item.roomId.toInt(),
                            startedBy = item.startedBy,
                            topic = item.topic?: EMPTY
                        )
                    )

                } else {
                    val errorResponse = Gson().fromJson(
                        apiResponse.errorBody()?.string(),
                        ConversationRoomResponse::class.java
                    )
                    navigation.postValue(ApiCallError(errorResponse.message ?: ""))

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun createRoom(topic: String,isFavouritePracticePartner:Boolean?=false,conversationQuestionId:Int?=null) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                var qId :Int? = null
                if (conversationQuestionId!=null && ( conversationQuestionId!=0 || conversationQuestionId != -1) ){
                    qId = conversationQuestionId
                }
                val createConversionRoomRequest =
                    CreateConversionRoomRequest(Mentor.getInstance().getId(), topic,isFavouritePracticePartner,qId)
                val apiResponse =
                    AppObjectController.conversationRoomsNetworkService.createConversationRoom(
                        createConversionRoomRequest
                    )
                if (apiResponse.isSuccessful) {
                    val response = apiResponse.body()
                    navigation.postValue(
                        OpenConversationLiveRoom(
                            response?.channelName,
                            response?.uid,
                            response?.token,
                            true,
                            response?.roomId,
                            response?.uid,
                            topic = topic
                            )
                    )
                } else {
                    val errorResponse = Gson().fromJson(
                        apiResponse.errorBody()?.string(),
                        ConversationRoomResponse::class.java
                    )
                    navigation.postValue(ApiCallError(errorResponse?.message ?: ""))
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }

        }
    }

    fun makeEnterExitConversationRoom(isEnter: Boolean) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = EnterExitConversionRoomRequest(Mentor.getInstance().getId())
                when (isEnter) {
                    true -> AppObjectController.conversationRoomsNetworkService.enterConversationRoom(
                        request
                    )
                    false -> AppObjectController.conversationRoomsNetworkService.exitConversationRoom(
                        request
                    )
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun getConvoRoomDetails(questionId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.conversationRoomsNetworkService.getConvoRoomQuestionDetails(questionId)
                if (response.isSuccessful&&response.body()!=null){
                    roomDetailsLivedata.postValue(response.body())
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun getPointsForConversationRoom(roomId: String?, conversationQuestionId: Int?) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.getSnackBarText(roomId = roomId,conversationQuestionId = conversationQuestionId.toString())
                if (response.pointsList?.get(0)?.isNotBlank()== true) {
                    points.postValue(response.pointsList.get(0))
                } else{
                    points.postValue(EMPTY)
                }
                PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getListRooms() {
        jobs += viewModelScope.launch((Dispatchers.IO)) {
            try {
                val response =
                    AppObjectController.conversationRoomsNetworkService.getRoomList()
                if (response.isSuccessful && response.body()!=null){
                    Log.d("ABCF", "getListRooms() called")
                    roomListLiveData.postValue(response.body())
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

}