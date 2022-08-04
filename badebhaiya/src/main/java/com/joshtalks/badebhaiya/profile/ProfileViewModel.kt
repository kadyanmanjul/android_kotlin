package com.joshtalks.badebhaiya.profile

import android.os.Message
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.datastore.BbDatastore
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.profile.response.ProfileResponse
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileViewModel : ViewModel() {
    //val userIdForOpenedProfile = MutableLiveData<String>()
    private val service = RetrofitInstance.profileNetworkService
    val isBadeBhaiyaSpeaker = ObservableBoolean(false)
    var profileUrl= ObservableField<String>()
    val repository = BBRepository()
    val userProfileData = MutableLiveData<ProfileResponse>()
    val userFullName = ObservableField<String>()
    val isBioTextAvailable = ObservableBoolean(false)
    val speakerProfileRoomsAdapter = FeedAdapter(fromProfile = true, coroutineScope = viewModelScope)
    var message = Message()
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    val speakerFollowed = MutableLiveData(false)
    val isSelfProfile = ObservableBoolean(false)
    var pubNubState = PubNubState.ENDED
    val roomRequestCount = MutableLiveData<Int>()


    private val commonRepository by lazy {
        CommonRepository()
    }
    val isNextEnabled = MutableLiveData<Boolean>(false)
    val openProfile = MutableLiveData<String>()
    var isSpeaker=false


    init {
//        collectPubNubState()
    }

    private fun collectPubNubState() {
        viewModelScope.launch {
            PubNubData.pubNubState.collect {
                pubNubState = it
            }
        }
    }

    fun openProfile(userId: String){
        openProfile.value = userId
    }

     fun saveProfileInfo(url: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestMap = mutableMapOf<String, String?>()
                requestMap["bio"] = url
                val response = repository.updateUserProfile(User.getInstance().userId, requestMap)
                if (response.isSuccessful) {
                    response.body()?.let {
                        User.getInstance().updateFromResponse(it)
                    }
                }
            } catch (ex: Exception) {
                Timber.d("BOTTOMSHEET saveProfileInfo: ${ex.message}")

            }
        }
    }



    fun updateFollowStatus(userId: String, isFromBBPage: Boolean, isFromDeeplink: Boolean, source:String?="PROFILE_SCREEN") {
        speakerFollowed.value?.let {
            if (it.not()) {
                viewModelScope.launch {
                    try {
                        val followRequest =
                            FollowRequest(userId, User.getInstance().userId,isFromBBPage,isFromDeeplink,source!!)
                        val response = service.updateFollowStatus(followRequest)
                        if (response.isSuccessful) {
                            speakerFollowed.value = true
                            userProfileData.value?.followersCount = userProfileData.value?.followersCount?.plus(1) ?: 0
                        }
                    } catch (ex: Exception) {

                    }
                }
            }
            else
            {
                viewModelScope.launch {
                    try {
                        val followRequest =
                            FollowRequest(userId, User.getInstance().userId, isFromBBPage, isFromDeeplink,source!!)
                        val response=service.updateUnfollowStatus(followRequest)
                        if(response.isSuccessful)
                        {
                            speakerFollowed.value=false
                            userProfileData.value?.followersCount=userProfileData.value?.followersCount?.minus(1)?:0
                        }
                    }
                    catch (ex:Exception){

                    }

                }
            }
        }
    }

    fun getRoomRequestCount(){
            commonRepository.roomRequestCount()
    }

    init {
        collectRoomRequestCount()
    }

    private fun collectRoomRequestCount(){
        viewModelScope.launch {
            BbDatastore.roomRequestCount.collectLatest {
                roomRequestCount.postValue(it)
            }
        }
    }

    fun getProfileForUser(userId: String, source: String) {
        if(!User.getInstance().isLoggedIn()){
            CoroutineScope(Dispatchers.IO).launch {
                try {
//                if(isFromDeepLink)
//                    updateFollowStatus(userId)
                    val response = repository.getProfileForUser(userId, source)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            userProfileData.postValue(it)
                            userFullName.set(it.firstName + " "+ it.lastName)
//                            profileUrl = it.profilePicUrl ?: ""
                            profileUrl.set(it.profilePicUrl?:"")
                            speakerFollowed.postValue(it.isSpeakerFollowed)
                            it.isSpeaker?.let { it1 -> isBadeBhaiyaSpeaker.set(it1) }
                            isSpeaker = it.isSpeaker == true
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
                                list.forEach { listItem ->
                                    listItem.currentTime = it.currentTime!!
                                }
                                speakerProfileRoomsAdapter.submitList(list.toList())
                            } else {
                                speakerProfileRoomsAdapter.submitList(emptyList())
                            }
                        }
                    } else {
                        showToast("Some Error Occured")
                    }
                } catch (ex: Exception) {
                    Timber.tag("YASHENDRA").d("getProfileForUser: Exception ${ex.message}")
                    ex.printStackTrace()
                    ex.cause
                    showToast("Some Error Occured")
                    speakerProfileRoomsAdapter.submitList(emptyList())
                }
            }
        }
        else
        {
            viewModelScope.launch {
                try {
//                if(isFromDeepLink)
//                    updateFollowStatus(userId)
                    val response = repository.getProfileForUser(userId, source)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            userProfileData.value=it
                            userFullName.set(it.firstName + " "+ it.lastName)
//                            profileUrl = it.profilePicUrl ?: ""
                            profileUrl.set(it.profilePicUrl?:"")
                            speakerFollowed.postValue(it.isSpeakerFollowed)
                            it.isSpeaker?.let { it1 -> isBadeBhaiyaSpeaker.set(it1) }
                            isSpeaker = it.isSpeaker == true
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
                            Log.i("pictureDelho", "getProfileForUser: ${it.profilePicUrl}")
                            if (list.isNullOrEmpty().not()) {
                                list.forEach { listItem ->
                                    listItem.currentTime = it.currentTime!!
                                }
                                speakerProfileRoomsAdapter.submitList(list.toList())
                            } else {
                                speakerProfileRoomsAdapter.submitList(emptyList())
                            }

                            Log.i("pictureView", "getProfileForUser: ${userProfileData.value}")
                        }
                    } else {
                        showToast("Some Error Occured")
                    }
                } catch (ex: Exception) {
                    Timber.tag("YASHENDRA").d("getProfileForUser: Exception ${ex.message}")
                    ex.printStackTrace()
                    ex.cause
                    showToast("Some Error Occured")
                    speakerProfileRoomsAdapter.submitList(emptyList())
                }
            }
        }
    }

    fun sendEvent(source: Impression) {
        viewModelScope.launch {
            try {
               service.sendEvent(source)
            } catch (e: Exception){

            }
        }
    }
//    fun deleteReminder(deleteReminderRequest: DeleteReminderRequest)
//    {
//        viewModelScope.launch {
//            try{
//                isLoading.set(true)
//                val res=convoRepo.deleteReminder(deleteReminderRequest)
//                if (res.isSuccessful && res.code()==200){
//                    speakerProfileRoomsAdapter.notifyDataSetChanged()
//                } else showToast("Error while Deleting Reminder")
//            } catch (ex:Exception){
//                ex.printStackTrace()
//                showToast("Error while Deleting Reminder")
//            } finally {
//                isLoading.set(false)
//            }
//        }
//    }

//    fun setReminder(reminderRequest: ReminderRequest) {
//        viewModelScope.launch {
//            try {
//                isLoading.set(true)
//                val res = convoRepo.setReminder(reminderRequest)
//                if (res.isSuccessful && res.code() == 201) {
//                    speakerProfileRoomsAdapter.notifyDataSetChanged()
//                } else showToast("Error while setting reminder")
//            } catch (ex: Exception) {
//                ex.printStackTrace()
//                showToast("Error while setting reminder")
//            } finally {
//                isLoading.set(false)
//            }
//        }
//    }
}