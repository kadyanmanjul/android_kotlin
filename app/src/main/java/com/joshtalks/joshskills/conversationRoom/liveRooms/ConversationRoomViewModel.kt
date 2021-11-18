package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.joshtalks.joshskills.conversationRoom.model.*
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.ApiCallError
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation.OpenConversationLiveRoom
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ConversationRoomViewModel (application: Application) : AndroidViewModel(application) {
    val navigation = MutableLiveData<ConversationRoomListingNavigation>()
    val roomDetailsLivedata = MutableLiveData<ConversationRoomDetailsResponse>()
    val roomListLiveData = MutableLiveData<RoomListResponse>()
    val points = MutableLiveData<String>()
    val isRoomEnded = MutableLiveData<Boolean>(false)
    var audienceList = MutableLiveData<ArrayList<LiveRoomUser>>()
    private val jobs = arrayListOf<Job>()

    fun updateInviteSentToUser(userId:Int){
        val audienceList = getAudienceList()
        if (audienceList.isNullOrEmpty()){
            return
        }
        val oldAudienceList:ArrayList<LiveRoomUser> = audienceList
        val user = oldAudienceList?.filter { it.id == userId }
        user?.get(0)?.let { it->
            oldAudienceList.remove(it)
            it.isInviteSent = true
            oldAudienceList.add(it)
            this.audienceList.postValue(oldAudienceList)
        }
    }

    fun updateHandRaisedToUser(userId:Int,isHandRaised:Boolean){
        val audienceList = getAudienceList()
        if (audienceList.isNullOrEmpty()){
            return
        }
        val oldAudienceList:ArrayList<LiveRoomUser> = audienceList
        val isUserPresent = oldAudienceList.any { it.id == userId }
        if (isUserPresent) {
            val roomUser = oldAudienceList.filter { it.id == userId }[0]
            oldAudienceList.remove(roomUser)
            roomUser.isHandRaised = isHandRaised
            if (isHandRaised.not()){
                roomUser.isInviteSent = false
            }
            oldAudienceList.add(roomUser)
            this.audienceList.postValue(oldAudienceList)
        }
    }

    fun updateAudienceList(audienceList:ArrayList<LiveRoomUser>){
        this.audienceList.postValue(audienceList)
    }

    fun getAudienceList() : ArrayList<LiveRoomUser>? = this.audienceList.value?: ArrayList<LiveRoomUser>()

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

}
