package com.joshtalks.badebhaiya.pubnub

import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.feed.NotificationView
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.*
import com.joshtalks.badebhaiya.liveroom.adapter.AudienceAdapter
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent
import com.joshtalks.badebhaiya.liveroom.adapter.SpeakerAdapter
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService
import com.joshtalks.badebhaiya.liveroom.viewmodel.*
import com.joshtalks.badebhaiya.pubnub.PubNubData._audienceList
import com.joshtalks.badebhaiya.pubnub.PubNubData._speakersList
import com.joshtalks.badebhaiya.pubnub.PubNubData.audienceList
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.UniqueList
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*

/**
    This object is responsible to handle PubNub Business Logic.
 */

object PubNubManager {

    @Volatile
    private var liveRoomProperties: StartingLiveRoomProperties? = null

    var moderatorName: String? = null

    var currentUser: LiveRoomUser? = null

    private lateinit var pubnub: PubNub
    private val message = Message()

    var moderatorUid: Int? = null

    private var pubNubCallback: SubscribeCallback? = null

    @Volatile
    private var speakersList = arraySetOf<LiveRoomUser>()

    @Volatile
    private var audienceList = arraySetOf<LiveRoomUser>()

    private val jobs = mutableListOf<Job>()

    private var pubNubEventJob: Job? = null

    @Volatile
    var isRoomActive = false

    fun warmUp(liveRoomProperties: StartingLiveRoomProperties) {
        this.liveRoomProperties = liveRoomProperties
        pubNubCallback = PubNubCallback()
    }

    fun getLiveRoomProperties(): StartingLiveRoomProperties {
        return if (liveRoomProperties != null){
            liveRoomProperties!!
        } else {
            endPubNub()
            StartingLiveRoomProperties()
        }
    }

    fun initPubNub() {
        val pnConf = PNConfiguration()
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = User.getInstance().userId
        pnConf.connectTimeout = 10
        pnConf.maximumConnections = Int.MAX_VALUE
        pnConf.isSecure = false
        pubnub = PubNub(pnConf)

        pubNubCallback?.let {
            pubnub.addListener(it)
        }

        jobs += CoroutineScope(Dispatchers.IO).launch {

        pubnub.subscribe().channels(
            listOf(liveRoomProperties?.channelName, liveRoomProperties?.agoraUid.toString())
        ).withPresence().execute()

        getLatestUserList()
        getSpeakerList()
        getAudienceList()
//        collectPubNubEvents()
        changePubNubState(PubNubState.STARTED)

        }

    }

    private fun changePubNubState(state: PubNubState){
        isRoomActive = state == PubNubState.STARTED
        jobs += CoroutineScope(Dispatchers.IO).launch{
            PubNubData._pubNubState.emit(state)
        }
    }

    fun endPubNub() {
        try {
            PubNubData.pubNubEvents.resetReplayCache()
            PubNubData._liveEvent.resetReplayCache()
            pubNubEventJob?.cancel()
            jobs.forEach {
                it.cancel()
            }
            jobs.clear()
            speakersList.clear()
            audienceList.clear()
            pubNubCallback?.let {
                pubnub.removeListener(it)
            }
            changePubNubState(PubNubState.ENDED)
        } catch (e: Exception){

        }

    }


    private fun getLatestUserList() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            try {

            pubnub.channelMembers.channel(liveRoomProperties?.channelName)
                ?.includeCustom(true)
                ?.async { result, status ->

                    Timber.d("Status hai => $status")

                    Timber.d("Memebers List Itne aye hai aur size => ${result?.data?.size}")
                    Timber.d("Memebers List Itne aye hai  => ${result?.data}")

                    val tempSpeakerList = ArraySet<LiveRoomUser>()
                    val tempAudienceList = ArraySet<LiveRoomUser>()
                    result?.data?.forEach {
                        Timber.d("Memebers List ind aur uid=> ${it.uuid}")
                        Timber.d("Memebers List ind aur banda=> $it")

                        Log.d("lvroom", "getLatestUserList() called with: memberList = $it ")
                        refreshUsersList(it.uuid.id, it.custom)?.let { user ->
                            Timber.d("Memebers List let k andr=> ${it.uuid}")

                            if (user.isSpeaker == true) {
                                Timber.d("Memebers List and speaker h=> ${it.uuid}")

                                tempSpeakerList.add(user)
                            } else {
                                Timber.d("Memebers List and audience h=> ${it.uuid}")

                                tempAudienceList.add(user)
                            }
                        }
                    }
                    // post to a shared flow instead of live data
                    Timber.d("THIS IS WITH MEMBERS LIST SPEAKER => $tempSpeakerList AND AUDIENCE => $tempAudienceList")
                    postToSpeakersList(tempSpeakerList)
                    postToAudienceList(tempAudienceList)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }

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
                if (liveRoomProperties?.moderatorId == null || liveRoomProperties?.moderatorId == 0) {
                    liveRoomProperties?.moderatorId = user.id
                }
                moderatorName = user.name
            }
            if (user.id == liveRoomProperties?.agoraUid) {
                currentUser = user
                //hideProgressBar()
            }
            return user

        }
        return null
    }

    fun getSpeakerList() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            PubNubData.speakerList
                .collect {
                speakersList = it
            }
        }
    }

    fun getAudienceList() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            PubNubData.audienceList.collect {
                audienceList = it
            }
        }
    }

    fun addNewUserToAudience(msg: JsonObject) {

        Timber.d("added new user to audience and json is => $msg")

        val data = msg["data"].asJsonObject

        Timber.d("added new user extracted data => $data")
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        if (data == null) {
            return
        }
        val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
        if (user.isModerator) {
            moderatorName = user.name
            moderatorUid = user.id
        }
        if (user.id == liveRoomProperties?.agoraUid) {
            currentUser = user
            message.what = HIDE_PROGRESSBAR
            postToLiveEvent(message)
        }
        if (user.isSpeaker == true) {
            val list = speakersList
            list.add(user)

            Timber.d("added new user it is a speaker => $data")

            postToSpeakersList(list)
            //speakerAdapter?.updateFullList(ArrayList(getSpeakerList()))
        } else {

            message.what = HIDE_SEARCHING_STATE
            postToLiveEvent(message)

            val list = audienceList
            list.add(user)
            Timber.d("added new user it is a audience => $data")

            postToAudienceList(list)
        }
    }

    private fun postToSpeakersList(list: ArraySet<LiveRoomUser>) {
        Timber.d("post to speaker list => $list")
        val distinctedList = list.reversed().distinctBy { it.userId }.reversed().toSet()
        jobs += CoroutineScope(Dispatchers.IO).launch {
            _speakersList.emit(ArraySet(distinctedList))
        }
    }

    private fun postToAudienceList(list: ArraySet<LiveRoomUser>) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            val distinctedList = list.reversed().distinctBy { it.userId }.reversed().toSet()
            _audienceList.emit(ArraySet(distinctedList))
        }
    }

    private fun postToLiveEvent(message: Message) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            PubNubData._liveEvent.emit(message)
        }
    }

    fun updateInviteSentToUserForSpeaker(userId: Int) {
        if (audienceList.isNullOrEmpty()) {
            return
        }
        val oldAudienceList = arraySetOf<LiveRoomUser>()
        oldAudienceList.addAll(audienceList)
        val user = oldAudienceList?.filter { it.id == userId }
        if (!user.isNullOrEmpty()){
            user?.get(0)?.let { it ->
                oldAudienceList.remove(it)
                it.isSpeakerAccepted = true
                oldAudienceList.add(it)

                postToAudienceList(oldAudienceList)
            }
        }
    }

    fun updateInviteSentToUser(userId: Int?) {
        if (audienceList.isNullOrEmpty()) {
            return
        }
        val oldAudienceList = arraySetOf<LiveRoomUser>()
        oldAudienceList.addAll(audienceList)
        val user = oldAudienceList?.filter { it.id == userId }
        user[0]?.let { it ->
            oldAudienceList.remove(it)
            it.isInviteSent = true
            oldAudienceList.add(it)
            postToAudienceList(oldAudienceList)
        }
    }

    private fun updateInviteSentToUserForAudience(userToMove: LiveRoomUser) {
//        val audienceList = audienceList
        if (audienceList.isNullOrEmpty()) {
            return
        }
//        val oldAudienceList = arraySetOf<LiveRoomUser>()
//        oldAudienceList.addAll(audienceList)
//        val user = oldAudienceList?.filter { it.id == userId }
//        user?.get(0)?.let { it ->
//            oldAudienceList.remove(it)
            userToMove.isSpeakerAccepted = false
//            oldAudienceList.add(it)
//
//            postToAudienceList(oldAudienceList)

//        }
    }

    fun updateHandRaisedToUser(userId: Int, isHandRaised: Boolean) {
        if (audienceList.isNullOrEmpty()) {
            return
        }
        val oldAudienceList = arraySetOf<LiveRoomUser>()
        oldAudienceList.addAll(audienceList)
        val isUserPresent = oldAudienceList.any { it.id == userId }
        if (isUserPresent) {
            val roomUser = oldAudienceList.filter { it.id == userId }[0]
            oldAudienceList.remove(roomUser)
            roomUser.isHandRaised = isHandRaised
            if (isHandRaised.not()) {
                roomUser.isInviteSent = false
            }
            oldAudienceList.add(roomUser)
            postToAudienceList(oldAudienceList)
        }
    }

    fun sendCustomMessage(state: JsonElement, channel: String = liveRoomProperties!!.channelName) {
        jobs += CoroutineScope(Dispatchers.IO).launch() {
            try {
                channel.let {
                    pubnub.publish()
                        .message(state)
                        ?.channel(it)
                        ?.sync()
                }
            } catch (e: Exception){
                e.printStackTrace()
            }

        }
    }

    fun unSubscribePubNub() {
        pubnub?.unsubscribeAll()
        endPubNub()
    }

    fun reconnectPubNub() {
        pubnub?.reconnect()
    }

    fun setChannelMemberStateForUuid(
        user: LiveRoomUser?,
        isMicOn: Boolean? = null,
        channelName: String?
    ) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            if (user == null || pubnub == null || user.id == null) {
                return@launch
            }
            val state = mutableMapOf<String, Any>()
            state.put("id", user!!.id!!)
            state.put("is_speaker", user.isSpeaker.toString())
            state.put("short_name", user.name ?: DEFAULT_NAME)
            state.put("photo_url", user.photoUrl ?: EMPTY)
            state.put("sort_order", user.sortOrder ?: 0)
            state.put("is_moderator", user.isModerator)
            state.put("is_mic_on", (isMicOn ?: user.isMicOn))
            state.put("is_speaking", user.isSpeaking)
            state.put("is_hand_raised", user.isHandRaised)
            state.put("user_id", user.userId)

            pubnub.setChannelMembers().channel(liveRoomProperties?.channelName)
                ?.uuids(
                    Arrays.asList(
                        PNUUID.uuidWithCustom(
                            user.id.toString(),
                            state as Map<String, Any>?
                        )
                    )
                )
                ?.includeCustom(true)
                ?.sync()
        }
    }

    private fun getAllUsersData(msgJson: JsonElement): LiveRoomUser {
        val data = msgJson.asJsonObject
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        return AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
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

            val isFromSpeakerList = speakersList.any { it.id == user.id }
            if (isFromSpeakerList) {
                val list = speakersList.filter { it.id == user.id }
                speakersList.removeAll(list)
                postToSpeakersList(speakersList)
            } else if (audienceList.any { it.id == user.id }) {
                val list = audienceList.filter { it.id == user.id }
                audienceList.removeAll(list)
                postToAudienceList(audienceList)
            }
        }
    }

    fun setHandRaisedForUser(userId: Int, isHandRaised: Boolean) {
        updateHandRaisedToUser(userId, isHandRaised)
        var newList = arraySetOf<LiveRoomUser>()
        newList.addAll(audienceList)
        val isOldUserPresent = newList.any { it.id == userId }
        if (isOldUserPresent) {
            val oldUser = newList.filter { it.id == userId }[0]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                newList.removeIf { it.id == userId }
            } else {
                newList.remove(oldUser)
            }
            oldUser.isHandRaised = isHandRaised
            if (isHandRaised) {
                oldUser.isSpeakerAccepted = false
            }
            newList.add(oldUser)
        }
        postToAudienceList(newList)
    }

    fun handRaisedByUser(msg: JsonObject) {
        if (msg.get("is_hand_raised").asBoolean) {
            message.what = SHOW_NOTIFICATION_FOR_INVITE_SPEAKER
            message.data = Bundle().apply {
                putString(NOTIFICATION_NAME, msg.get("short_name").asString)
                putInt(NOTIFICATION_ID, msg.get("id").asInt)
                putParcelable(
                    NOTIFICATION_TYPE,
                    NotificationView.ConversationRoomNotificationState.HAND_RAISED
                )
            }
            postToLiveEvent(message)
            setHandRaisedForUser(msg.get("id").asInt, true)
        } else {
            setHandRaisedForUser(msg.get("id").asInt, false)
        }
    }

    fun moveToSpeaker(msg: JsonObject) {
        msg.get("id").asInt?.let { agoraId ->
            val user = audienceList.filter { it.id == agoraId }
            if (user.isNotEmpty()) {
                val userToMove = user.get(0)
                Log.d("lvroom", "moveToSpeaker audience list before removing => $audienceList")
                audienceList.remove(userToMove)
                postToAudienceList(audienceList)
                Log.d("lvroom", "moveToSpeaker audience list => $audienceList")
                userToMove.isSpeaker = true
                userToMove.isMicOn = false
                userToMove.isHandRaised = false
                userToMove.isInviteSent = true
                updateInviteSentToUserForAudience(userToMove)
                speakersList.add(userToMove)
                postToSpeakersList(speakersList)
                message.what = MOVE_TO_SPEAKER
                var bundle = Bundle().apply {
                    putParcelable(NOTIFICATION_USER, userToMove)
                }
                if (isModerator()) {
                    bundle.putString(NOTIFICATION_NAME, msg.get("short_name")?.asString)
                }
                message.data = bundle
                postToLiveEvent(message)
            }
        }
    }

     fun postToPubNubEvent(data: ConversationRoomPubNubEventBus) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            PubNubData.pubNubEvents.emit(data)
        }

    }

    fun moveToAudience(msg: JsonObject) {
        msg.get("id").asInt?.let { agoraId ->
            val user = speakersList.filter { it.id == agoraId }
            if (user.isNotEmpty()) {
                val userToMove = user[0]
                speakersList.remove(userToMove)
                postToSpeakersList(speakersList)
                userToMove.isSpeaker = false
                userToMove.isHandRaised = false
                userToMove.isInviteSent = false
                audienceList.add(userToMove)
                postToAudienceList(audienceList)

                message.what = MOVE_TO_AUDIENCE
                message.data = Bundle().apply {
                    putParcelable(NOTIFICATION_USER, userToMove)
                }
                postToLiveEvent(message)
            }
        }
    }

    fun pauseRoomDataCollection() {
        PubNubData.pubNubEvents.resetReplayCache()
        pubNubEventJob?.cancel()
    }

    fun changeMicStatus(eventObject: JsonObject) {
        Log.d("ABC2", "presence() called mic_status_changes")
        val isMicOn = eventObject.get("is_mic_on").asBoolean

        message.what = CHANGE_MIC_STATUS
        message.data = Bundle().apply {
            putBoolean(NOTIFICATION_BOOLEAN, isMicOn)
            putInt(NOTIFICATION_ID, eventObject.get("id").asInt)
        }
        postToLiveEvent(message)


        val userId = eventObject.get("id").asInt

        val newList: ArraySet<LiveRoomUser> = ArraySet(speakersList)
        val isOldUserPresent = newList.any { it.id == userId }
        if (isOldUserPresent) {
            val oldUser = newList.filter { it.id == userId }
            newList.removeAll(oldUser)
            oldUser[0].isMicOn = isMicOn
            newList.add(oldUser[0])
        }
        postToSpeakersList(newList)

    }

    fun removeUserWhenLeft(
        uid: Int,
        speakerAdapter: SpeakerAdapter?,
        audienceAdapter: AudienceAdapter?
    ) {
        if (speakersList.any { it.id == uid }) {
            val user = speakersList.filter { it.id == uid }
            speakersList.removeAll(user)
            CoroutineScope(Dispatchers.Main).launch {
                speakerAdapter?.updateFullList(ArrayList(speakersList))
            }
        } else if (audienceList.any { it.id == uid }) {
            val user = audienceList.filter { it.id == uid }
            audienceList.removeAll(user)
            CoroutineScope(Dispatchers.Main).launch {
                audienceAdapter?.updateFullList(ArrayList(audienceList))
            }
            postToAudienceList(audienceList)
        }
        message.what = LIST_UPDATE
        postToLiveEvent(message)
    }


    fun callWebRtcService() {
        Log.d(
            "ABC2",
            "conversationRoomJoin() called with: token = ${liveRoomProperties?.token}, channelName = ${liveRoomProperties?.channelName}, uid = ${liveRoomProperties?.agoraUid}, moderatorId = ${liveRoomProperties?.moderatorId}, channelTopic = ${liveRoomProperties?.channelTopic}, roomId = ${liveRoomProperties?.roomId}, roomQuestionId = ${liveRoomProperties?.roomQuestionId}"
        )
        ConvoWebRtcService.conversationRoomJoin(
            liveRoomProperties?.token,
            liveRoomProperties?.channelName,
            liveRoomProperties?.agoraUid,
            liveRoomProperties?.moderatorId,
            liveRoomProperties?.channelTopic,
            liveRoomProperties?.roomId,
            liveRoomProperties?.roomQuestionId,
            liveRoomProperties?.isRoomCreatedByUser
        )
    }

    fun collectPubNubEvents() {
        pubNubEventJob = CoroutineScope(Dispatchers.IO).launch {
            PubNubData.pubNubEvents.collect {
                when (it.action) {
                    PubNubEvent.CREATE_ROOM, PubNubEvent.JOIN_ROOM -> PubNubManager.addNewUserToAudience(
                        it.data
                    )
                    PubNubEvent.LEAVE_ROOM -> PubNubManager.removeUser(it.data)
                    PubNubEvent.END_ROOM -> PubNubManager.leaveRoom()
                    PubNubEvent.IS_HAND_RAISED -> PubNubManager.handRaisedByUser(it.data)
                    PubNubEvent.INVITE_SPEAKER -> PubNubManager.inviteUserToSpeaker()
                    PubNubEvent.MOVE_TO_SPEAKER -> PubNubManager.moveToSpeaker(it.data)
                    PubNubEvent.MOVE_TO_AUDIENCE -> PubNubManager.moveToAudience(it.data)
                    PubNubEvent.MIC_STATUS_CHANGES -> PubNubManager.changeMicStatus(it.data)
                    else -> {

                    }
                }
            }
        }
    }

    private fun leaveRoom() {
        message.what = LEAVE_ROOM
        postToLiveEvent(message)
    }

    private fun inviteUserToSpeaker() {
        message.what = SHOW_NOTIFICATION_FOR_USER_TO_JOIN
        postToLiveEvent(message)
    }

    private fun isModerator(): Boolean =
        liveRoomProperties?.moderatorId == liveRoomProperties?.agoraUid


}