package com.joshtalks.joshskills.conversationRoom.liveRooms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomNavigation.ApiCallError
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomNavigation.ExitRoom
import com.joshtalks.joshskills.conversationRoom.model.JoinConversionRoomRequest
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.launch

class ConversationLiveRoomViewModel : ViewModel() {
    val navigation = MutableLiveData<ConversationLiveRoomNavigation>()

    fun leaveEndRoom(isRoomCreatedByUser: Boolean, roomId: Int?) {
        val request = JoinConversionRoomRequest(Mentor.getInstance().getId(), roomId ?: 0)
        viewModelScope.launch {
            when (isRoomCreatedByUser) {
                true -> {
                    callExitRoom(request, isRoomCreatedByUser)
                }
                false -> {
                    callExitRoom(request, isRoomCreatedByUser)
                }
            }
        }
    }

    private suspend fun callExitRoom(
        request: JoinConversionRoomRequest,
        isRoomCreatedByUser: Boolean
    ) {
        try {
            val apiResponse = when (isRoomCreatedByUser) {
                true -> {
                    AppObjectController.conversationRoomsNetworkService.endConversationLiveRoom(
                        request
                    )
                }
                false -> {
                    AppObjectController.conversationRoomsNetworkService.leaveConversationLiveRoom(
                        request
                    )
                }
            }

            val response = apiResponse.body()
            if (apiResponse.isSuccessful) {
                navigation.postValue(ExitRoom())
            } else {
                navigation.postValue(ApiCallError())
            }
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }
}