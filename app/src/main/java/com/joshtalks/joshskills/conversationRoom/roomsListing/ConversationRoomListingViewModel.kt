package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.conversationRoom.model.CreateConversionRoomRequest
import com.joshtalks.joshskills.conversationRoom.model.JoinConversionRoomRequest
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationRoomListingViewModel : ViewModel() {
    fun joinRoom(item: ConversationRoomsListingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val joinRoomRequest = JoinConversionRoomRequest(Mentor.getInstance().getId(), 1)

                val response = AppObjectController.conversationRoomsNetworkService.joinConversationRoom(joinRoomRequest)
                if (response.isSuccessful){
                    Log.d("ConversationViewModel", "Join Room Api Success")
                }else{
                    Log.d("ConversationViewModel", "Join Room Api Failure")
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun createRoom(topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createConversionRoomRequest = CreateConversionRoomRequest(Mentor.getInstance().getId(), topic)
                val response = AppObjectController.conversationRoomsNetworkService.createConversationRoom(createConversionRoomRequest)
                if (response.isSuccessful){
                    Log.d("ConversationViewModel", "Create Room Api Success")
                }else{
                    Log.d("ConversationViewModel", "Create Room Api Failure")
                }
            }catch (ex: Throwable){
                ex.showAppropriateMsg()
            }

        }
    }


}