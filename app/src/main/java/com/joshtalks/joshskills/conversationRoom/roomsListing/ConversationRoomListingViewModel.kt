package com.joshtalks.joshskills.conversationRoom.roomsListing

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.joshtalks.joshskills.conversationRoom.model.ConversationRoomDetailsResponse
import com.joshtalks.joshskills.conversationRoom.model.ConversationRoomResponse
import com.joshtalks.joshskills.conversationRoom.model.CreateConversionRoomRequest
import com.joshtalks.joshskills.conversationRoom.model.EnterExitConversionRoomRequest
import com.joshtalks.joshskills.conversationRoom.model.JoinConversionRoomRequest
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.ApiCallError
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.OpenConversationLiveRoom
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.HAS_SEEN_CONVO_ROOM_POINTS
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConversationRoomListingViewModel : ViewModel() {
    val navigation = MutableLiveData<ConversationRoomListingNavigation>()
    val roomDetailsLivedata = MutableLiveData<ConversationRoomDetailsResponse>()
    val points = MutableLiveData<String>()
    val isRoomEnded = MutableLiveData<Boolean>(false)

    fun joinRoom(item: ConversationRoomsListingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val joinRoomRequest =
                    JoinConversionRoomRequest(Mentor.getInstance().getId(), item.room_id ?: 0,item.conversationRoomQuestionId)

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
                            item.started_by == response?.uid,
                            response?.roomId ?: item.room_id
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createConversionRoomRequest =
                    CreateConversionRoomRequest(Mentor.getInstance().getId(), topic,isFavouritePracticePartner,conversationQuestionId)
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
                            response?.roomId
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
        viewModelScope.launch(Dispatchers.IO) {
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

    fun checkRoomsAvailableOrNot(value: QuerySnapshot?) {
        if (value == null || value.isEmpty) {
            navigation.postValue(ConversationRoomListingNavigation.NoRoomAvailable)
        } else {
            navigation.postValue(ConversationRoomListingNavigation.AtleastOneRoomAvailable)
        }
    }

    fun getConvoRoomDetails(questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            delay(200)
            try {
                val response =
                    AppObjectController.chatNetworkService.getSnackBarText(roomId = roomId,conversationQuestionId = conversationQuestionId.toString())
                if (response.pointsList?.get(0)?.isNotBlank()== true) {
                    points.postValue(response.pointsList.get(0))
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun endRoom(roomId: String?,conversationQuestionId:Int?=null) {
        CoroutineScope(Dispatchers.IO).launch {
            val request =
                JoinConversionRoomRequest(Mentor.getInstance().getId(), roomId?.toInt() ?: 0,conversationQuestionId)
            val response =
                AppObjectController.conversationRoomsNetworkService.endConversationLiveRoom(request)
            if (response.isSuccessful) {
                isRoomEnded.postValue(true)
                PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS,false)
            }
        }
    }


}