package com.joshtalks.joshskills.ui.lesson.conversationRoom.liveRooms

import android.app.Application
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.collection.ArraySet
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.ui.lesson.conversationRoom.notification.NotificationView
import com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing.ConversationRoomListingNavigation
import com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing.ConversationRoomListingNavigation.ApiCallError
import com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing.ConversationRoomListingNavigation.OpenConversationLiveRoom
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.DEFAULT_NAME
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.repository.local.eventbus.ConversationRoomPubNubEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.lesson.conversationRoom.model.*
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.member.PNUUID
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

const val NOTIFICATION_ID = "notification_id"
const val NOTIFICATION_BOOLEAN = "notification_boolean"
const val NOTIFICATION_NAME = "notification_name"
const val NOTIFICATION_TYPE = "notification_type"
const val NOTIFICATION_USER = "notification_type"

class ConversationRoomViewModel(application: Application) : AndroidViewModel(application) {

    val navigation = MutableLiveData<ConversationRoomListingNavigation>()
    val roomDetailsLivedata = MutableLiveData<ConversationRoomDetailsResponse>()
    val roomListLiveData = MutableLiveData<RoomListResponse>()
    val points = MutableLiveData<String>()
    var audienceList = MutableLiveData<ArraySet<LiveRoomUser>>()
    var speakersList = MutableLiveData<ArraySet<LiveRoomUser>>()
    private val jobs = arrayListOf<Job>()
    private var moderatorUid: Int? = null
    private var moderatorName: String? = null
    private var currentUser: LiveRoomUser? = null
    private var replaySubject = ReplaySubject.create<Any>()
    private var pubnub: PubNub? = null
    private var agoraUid: Int? = null
    var isPubNubUsersFetched: MutableLiveData<Boolean> = MutableLiveData(false)
    var message = Message()
    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()

    fun isModerator() = moderatorUid == agoraUid

    fun getModeratorId() = moderatorUid
    fun setModeratorId(moderatorUid: Int?) {
        this.moderatorUid = moderatorUid
    }

    fun getAgoraUid() = agoraUid
    fun setAgoraUid(agoraUid: Int?) {
        this.agoraUid = agoraUid
    }

    fun getModeratorName() = moderatorName
    fun setModeratorName(moderatorName: String?) {
        this.moderatorName = moderatorName
    }

    fun getSpeakerList() = this.speakersList.value ?: ArraySet<LiveRoomUser>()
    fun setSpeakerList(list: ArraySet<LiveRoomUser>) = this.speakersList.postValue(list)

    fun getAudienceList() = this.audienceList.value ?: ArraySet<LiveRoomUser>()
    fun setAudienceList(list: ArraySet<LiveRoomUser>) = this.audienceList.postValue(list)

    fun getCurrentUser() = currentUser
    fun setCurrentUser(currentUser: LiveRoomUser?) {
        this.currentUser = currentUser
    }

    fun updateInviteSentToUser(userId: Int) {
        val audienceList = getAudienceList()
        if (audienceList.isNullOrEmpty()) {
            return
        }
        val oldAudienceList: ArraySet<LiveRoomUser> = audienceList
        val user = oldAudienceList?.filter { it.id == userId }
        user?.get(0)?.let { it ->
            oldAudienceList.remove(it)
            it.isInviteSent = true
            oldAudienceList.add(it)
            this.audienceList.postValue(oldAudienceList)
        }
    }

    fun updateHandRaisedToUser(userId: Int, isHandRaised: Boolean) {
        val audienceList = getAudienceList()
        if (audienceList.isNullOrEmpty()) {
            return
        }
        val oldAudienceList: ArraySet<LiveRoomUser> = audienceList
        val isUserPresent = oldAudienceList.any { it.id == userId }
        if (isUserPresent) {
            val roomUser = oldAudienceList.filter { it.id == userId }[0]
            oldAudienceList.remove(roomUser)
            roomUser.isHandRaised = isHandRaised
            if (isHandRaised.not()) {
                roomUser.isInviteSent = false
            }
            oldAudienceList.add(roomUser)
            this.audienceList.postValue(oldAudienceList)
        }
    }

    fun updateAudienceList(audienceList: ArraySet<LiveRoomUser>) {
        this.audienceList.postValue(audienceList)
    }

    fun joinRoom(item: RoomListResponseItem) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                var qId: Int? = null
                if (item.conversationRoomQuestionId != null && (item.conversationRoomQuestionId != 0 || item.conversationRoomQuestionId != -1)) {
                    qId = item.conversationRoomQuestionId
                }
                val joinRoomRequest =
                    JoinConversionRoomRequest(
                        Mentor.getInstance().getId(),
                        item.roomId.toInt(),
                        qId
                    )

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
                            item.startedBy ?: 0 == response?.uid,
                            response?.roomId?.toInt() ?: item.roomId.toInt(),
                            startedBy = item.startedBy,
                            topic = item.topic ?: EMPTY
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

    fun initPubNub(channelName: String?) {
        val pnConf = PNConfiguration()
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = Mentor.getInstance().getId()
        pnConf.isSecure = false
        pubnub = PubNub(pnConf)

        pubnub?.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                val msg = pnMessageResult.message.asJsonObject
                val act = msg["action"].asString
                try {
                    if (msg != null) {
                        replaySubject.toSerialized()
                            .onNext(ConversationRoomPubNubEventBus(PubNubEvent.valueOf(act), msg))
                    }
                } catch (ex: Exception) {
                    LogException.catchException(ex)
                }
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
            }

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

            override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

            override fun channel(
                pubnub: PubNub,
                pnChannelMetadataResult: PNChannelMetadataResult
            ) {
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
            }

            override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
        })

        pubnub?.subscribe()?.channels(
            Arrays.asList(channelName, agoraUid.toString())
        )?.withPresence()
            ?.execute()

        getLatestUserList(channelName)
    }

    private fun getLatestUserList(channelName: String?) {

        pubnub?.channelMembers
            ?.channel(channelName)
            ?.includeCustom(true)
            ?.async { result, status ->
                Log.d("ABC2", "getLatestUserList() called with: result = $result, status = $status")
                val tempSpeakerList = ArraySet<LiveRoomUser>()
                val tempAudienceList = ArraySet<LiveRoomUser>()
                result?.data?.forEach {
                    refreshUsersList(it.uuid.id, it.custom)?.let { user ->
                        if (user.isSpeaker == true) {
                            tempSpeakerList.add(user)
                        } else {
                            tempAudienceList.add(user)
                        }
                    }
                }
                speakersList.postValue(tempSpeakerList)
                audienceList.postValue(tempAudienceList)
                isPubNubUsersFetched.postValue(true)
            }
    }

    private fun refreshUsersList(uid: String, state: Any): LiveRoomUser? {
        if (uid.isBlank()) {
            return null
        }
        //val user = getAllUsersData(state)
        Log.d("ABC2", "refreshUsersList() called with: state = $state")

        if (state is JsonElement) {
            val user = getAllUsersData(state)
            user.id = uid.toInt()
            Log.d("ABC2", "refreshUsersList() called with: user = $user")

            if (user.isModerator) {
                if (moderatorUid == null || moderatorUid == 0) {
                    setModeratorId(user.id)
                }
                moderatorName = user.name
            }
            if (user.id == agoraUid) {
                currentUser = user
                //hideProgressBar()
            }
            return user

        }
        return null
    }

    private fun getAllUsersData(msgJson: JsonElement): LiveRoomUser {
        val data = msgJson.asJsonObject
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        return AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
    }

    fun setChannelMemberStateForUuid(
        user: LiveRoomUser?,
        isMicOn: Boolean? = null,
        channelName: String?
    ) {
        if (user == null || pubnub == null || user.id == null) {
            return
        }
        val state = mutableMapOf<String, Any>()
        state.put("id", user.id!!)
        state.put("is_speaker", user.isSpeaker.toString())
        state.put("name", user.name ?: DEFAULT_NAME)
        state.put("photo_url", user.photoUrl ?: EMPTY)
        state.put("sort_order", user.sortOrder ?: 0)
        state.put("is_moderator", user.isModerator)
        state.put("is_mic_on", (isMicOn ?: user.isMicOn))
        state.put("is_speaking", user.isSpeaking)
        state.put("is_hand_raised", user.isHandRaised)
        state.put("mentor_id", user.mentorId)

        pubnub?.setChannelMembers()?.channel(channelName)
            ?.uuids(
                Arrays.asList(
                    PNUUID.uuidWithCustom(
                        user.id.toString(),
                        state as Map<String, Any>?
                    )
                )
            )
            ?.includeCustom(true)
            ?.async { result, status ->

            }
    }

    fun sendCustomMessage(state: JsonElement, channelName: String? = null) {
        channelName?.let {
            pubnub?.publish()
                ?.message(state)
                ?.channel(channelName)
                ?.async(object : PNCallback<PNPublishResult> {
                    override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                        if (status.isError.not()) {
                            Log.d(
                                "ABC2",
                                "onResponse() called with: state = $state, channelName = $channelName result = $result"
                            )
                        }
                    }
                })
        }
    }

    fun unSubscribePubNub() {
        pubnub?.unsubscribeAll()
    }

    fun reconnectPubNub() {
        pubnub?.reconnect()
    }

    fun getReplaySubject() = replaySubject
    fun setReplaySubject(replaySubject: ReplaySubject<Any>) {
        this.replaySubject = replaySubject
    }

    fun getReplayDisposable(): Disposable {
        return replaySubject.ofType(ConversationRoomPubNubEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Log.d(
                    "ABC2",
                    "inside disposable called ${it.action.name} ${it.data}  isPubNubObserverAdded: ${isPubNubUsersFetched}"
                )
                when (it.action) {
                    PubNubEvent.CREATE_ROOM -> addNewUserToAudience(it.data)
                    PubNubEvent.JOIN_ROOM -> addNewUserToAudience(it.data)
                    PubNubEvent.LEAVE_ROOM -> removeUser(it.data)
                    PubNubEvent.END_ROOM -> leaveRoom()
                    PubNubEvent.IS_HAND_RAISED -> handRaisedByUser(it.data)
                    PubNubEvent.INVITE_SPEAKER -> inviteUserToSpeaker()
                    PubNubEvent.MOVE_TO_SPEAKER -> moveToSpeaker(it.data)
                    PubNubEvent.MOVE_TO_AUDIENCE -> moveToAudience(it.data)
                    PubNubEvent.MIC_STATUS_CHANGES -> changeMicStatus(it.data)
                    else -> {

                    }
                }
            }
    }

    private fun addNewUserToAudience(msg: JsonObject) {
        val data = msg["data"].asJsonObject
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        if (data == null) {
            return
        }
        val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
        if (user.isModerator) {
            setModeratorId(user.id)
            setModeratorName(user.name)
        }
        if (user.id == agoraUid) {
            setCurrentUser(user)
            message.what = HIDE_PROGRESSBAR
            singleLiveEvent.postValue(message)

        }
        if (user.isSpeaker == true) {
            val list = getSpeakerList()
            list.add(user)
            setSpeakerList(list)
            //speakerAdapter?.updateFullList(ArrayList(getSpeakerList()))
        } else {

            message.what = HIDE_SEARCHING_STATE
            singleLiveEvent.postValue(message)

            val list = getAudienceList()
            list.add(user)
            setAudienceList(list)
        }
    }

    fun removeUser(msg: JsonObject) {
        val data: JsonObject? = msg["data"].asJsonObject
        data?.let {
            Log.d("ABC2", "removeUser() called ${data.get("id")}")
            val matType = object : TypeToken<LiveRoomUser>() {}.type
            if (data == null) {
                return
            }
            val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)

            val isFromSpeakerList = getSpeakerList().any { it.id == user.id }
            if (isFromSpeakerList) {
                val list = getSpeakerList().filter { it.id == user.id }
                getSpeakerList().removeAll(list)
                setSpeakerList(getSpeakerList())
            } else if (getAudienceList().any { it.id == user.id }) {
                val list = getAudienceList().filter { it.id == user.id }
                getAudienceList().removeAll(list)
                setAudienceList(getAudienceList())
            }
        }
    }

    private fun leaveRoom() {
        message.what = LEAVE_ROOM
        singleLiveEvent.postValue(message)
    }

    private fun handRaisedByUser(msg: JsonObject) {
        if (msg.get("is_hand_raised").asBoolean) {
            message.what = SHOW_NOTIFICATION_FOR_INVITE_SPEAKER
            message.data = Bundle().apply {
                putString(NOTIFICATION_NAME, msg.get("name").asString)
                putInt(NOTIFICATION_ID, msg.get("id").asInt)
                putParcelable(
                    NOTIFICATION_TYPE,
                    NotificationView.ConversationRoomNotificationState.HAND_RAISED
                )
            }
            singleLiveEvent.postValue(message)


            setHandRaisedForUser(msg.get("id").asInt, true)
        } else {
            setHandRaisedForUser(msg.get("id").asInt, false)
        }
    }

    private fun setHandRaisedForUser(userId: Int, isHandRaised: Boolean) {
        updateHandRaisedToUser(userId, isHandRaised)
        val newList: ArraySet<LiveRoomUser> = ArraySet(getAudienceList())
        val isOldUserPresent = newList.any { it.id == userId }
        if (isOldUserPresent) {
            val oldUser = newList.filter { it.id == userId }
            newList.removeAll(oldUser)
            oldUser[0].isHandRaised = isHandRaised
            newList.add(oldUser[0])
        }
        setAudienceList(newList)
    }

    private fun moveToSpeaker(msg: JsonObject) {
        msg.get("id").asInt?.let { agoraId ->
            val user = getAudienceList().filter { it.id == agoraId }
            if (user.size > 0) {
                val userToMove = user.get(0)
                getAudienceList().remove(userToMove)
                setAudienceList(getAudienceList())
                userToMove.isSpeaker = true
                userToMove.isMicOn = false
                userToMove.isHandRaised = false
                userToMove.isInviteSent = true
                getSpeakerList().add(userToMove)
                setSpeakerList(getSpeakerList())
                message.what = MOVE_TO_SPEAKER
                var bundle = Bundle().apply {
                    putParcelable(NOTIFICATION_USER, userToMove)
                }
                if (isModerator()) {
                    bundle.putString(NOTIFICATION_NAME, msg.get("name")?.asString)
                }
                message.data = bundle
                singleLiveEvent.postValue(message)
            }
        }
    }

    private fun moveToAudience(msg: JsonObject) {
        msg.get("id").asInt?.let { agoraId ->
            val user = getSpeakerList().filter { it.id == agoraId }
            if (user.size > 0) {
                val userToMove = user.get(0)
                getSpeakerList().remove(userToMove)
                setSpeakerList(getSpeakerList())
                userToMove.isSpeaker = false
                userToMove.isHandRaised = false
                userToMove.isInviteSent = false
                getAudienceList().add(userToMove)
                setAudienceList(getAudienceList())

                message.what = MOVE_TO_AUDIENCE
                message.data = Bundle().apply {
                    putParcelable(NOTIFICATION_USER, userToMove)
                }
                singleLiveEvent.postValue(message)
            }
        }
    }

    private fun changeMicStatus(eventObject: JsonObject) {
        Log.d("ABC2", "presence() called mic_status_changes")
        val isMicOn = eventObject.get("is_mic_on").asBoolean

        message.what = CHANGE_MIC_STATUS
        message.data = Bundle().apply {
            putBoolean(NOTIFICATION_BOOLEAN, isMicOn)
            putInt(NOTIFICATION_ID, eventObject.get("id").asInt)
        }
        singleLiveEvent.postValue(message)


        val userId = eventObject.get("id").asInt

        val newList: ArraySet<LiveRoomUser> = ArraySet(speakersList.value)
        val isOldUserPresent = newList.any { it.id == userId }
        if (isOldUserPresent) {
            val oldUser = newList.filter { it.id == userId }
            newList.removeAll(oldUser)
            oldUser[0].isMicOn = isMicOn
            newList.add(oldUser[0])
        }
        speakersList.postValue(newList)

    }

    private fun inviteUserToSpeaker() {
        message.what = SHOW_NOTIFICATION_FOR_USER_TO_JOIN
        singleLiveEvent.postValue(message)
    }

}
