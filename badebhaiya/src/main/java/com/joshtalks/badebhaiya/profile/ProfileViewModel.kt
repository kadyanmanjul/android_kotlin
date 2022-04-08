package com.joshtalks.badebhaiya.profile

import android.os.Message
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    val userIdForOpenedProfile = MutableLiveData<String>()
    private val service = RetrofitInstance.profileNetworkService
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    val repository = BBRepository()
    val userProfileData = MutableLiveData<ProfileResponse>()
    val isBioTextAvailable = ObservableBoolean(false)
    val speakerProfileRoomsAdapter = FeedAdapter()
    var message = Message()
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    val speakerFollowed = MutableLiveData(false)
    val isSelfProfile = ObservableBoolean(false)

    fun updateFollowStatus() {
        viewModelScope.launch {
            try {
                val followRequest =
                    FollowRequest(userIdForOpenedProfile.value ?: "", User.getInstance().userId)
                val response = service.updateFollowStatus(followRequest)
                Log.i("ayushg", "updateFollowStatus: response: $response , succ: ${response.isSuccessful}")
                if (response.isSuccessful) {
                    speakerFollowed.value = true
                }
            } catch (ex: Exception) {

            }
        }
    }

    fun getProfileForUser(userId: String) {
        viewModelScope.launch {
            try {
                userIdForOpenedProfile.postValue(userId)
                val response = repository.getProfileForUser(userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        userProfileData.postValue(it)
                        isBadeBhaiyaSpeaker.set(it.isSpeaker)
                        isBadeBhaiyaSpeaker.notifyChange()
                        isBioTextAvailable.set(it.bioText.isNullOrEmpty().not())
                        isSelfProfile.set(it.userId == User.getInstance().userId)
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
                        if (list.isNullOrEmpty().not()) {
                            speakerProfileRoomsAdapter.submitList(list)
                        }
                    }
                }
            } catch(ex: Exception) {
                speakerProfileRoomsAdapter.submitList(null)
            }
        }
    }
}