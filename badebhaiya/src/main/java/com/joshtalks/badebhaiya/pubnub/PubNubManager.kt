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
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.core.showToast
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
import com.joshtalks.badebhaiya.pubnub.PubNubData.moderatorStatus
import com.joshtalks.badebhaiya.pubnub.PubNubData.eventsMap
import com.joshtalks.badebhaiya.pubnub.fallback.FallbackManager
import com.joshtalks.badebhaiya.repository.PubNubExceptionRepository
import com.joshtalks.badebhaiya.repository.model.PubNubExceptionRequest
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.UniqueList
import com.joshtalks.badebhaiya.utils.Utils
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult
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
import java.net.SocketTimeoutException
import java.util.*

/**
    This object is responsible to handle PubNub Business Logic.
 */

object PubNubManager {

    @Volatile
    private var liveRoomProperties: StartingLiveRoomProperties? = null
    private var channelName:String?=null

    val networkFlow= MutableSharedFlow<Boolean>()

    var moderatorName: String? = null

    var roomJoiningTime: Long = 0L

    var currentUser: LiveRoomUser? = null

    private lateinit var pubnub: PubNub
    private lateinit var isJoinedPubnub:PubNub
    private val message = Message()

    var moderatorUid: Int? = null

    private var pubNubCallback: SubscribeCallback? = null

    private var waitingCallback: SubscribeCallback?=null

    @Volatile
    private var speakersList = arraySetOf<LiveRoomUser>()

    @Volatile
    private var audienceList = arraySetOf<LiveRoomUser>()

    private val jobs = mutableListOf<Job>()

    private var pubNubEventJob: Job? = null

    private var reconnecting = false

    @Volatile
    var isRoomActive = false

    fun warmUp(liveRoomProperties: StartingLiveRoomProperties) {
        Log.i("MODERATORSTATUS", "warmUp: $liveRoomProperties")
        this.liveRoomProperties = liveRoomProperties
        pubNubCallback = PubNubCallback()
    }

    fun warmUpChannel(channelName: String){
        Log.i("MODERATORSTATUS", "warmUpChannel: $channelName")
        this.channelName= channelName
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
        reconnecting = false
        roomJoiningTime = System.currentTimeMillis()
        val pnConf = PNConfiguration(User.getInstance().userId)
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = User.getInstance().userId
        pnConf.connectTimeout = 10
        pnConf.maximumConnections = Int.MAX_VALUE
        pnConf.isSecure = false
        pubnub = PubNub(pnConf)
        Log.i("MODERATORSTATUS", "initPubNub: ")

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

        FallbackManager.start()

        }

    }

    fun initSpeakerJoined(){
        Log.i("MODERATORSTATUS", "initSpeakerJoined: ${channelName}waitingRoom")
        val pnConf = PNConfiguration(User.getInstance().userId)
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = User.getInstance().userId
        pnConf.connectTimeout = 10
        pnConf.maximumConnections = Int.MAX_VALUE
        pnConf.isSecure = false
        waitingCallback=WaitingCallback()
        isJoinedPubnub = PubNub(pnConf)
        waitingCallback?.let {
            isJoinedPubnub.addListener(it)
        }

        jobs += CoroutineScope(Dispatchers.IO).launch {

            isJoinedPubnub.subscribe().channels(
                listOf("${channelName}waitingRoom")
            ).withPresence().execute()

        }
    }

    private fun getSpeakerStatus() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            try {
                pubnub.channelMembers.channel(liveRoomProperties?.channelName+"waitingRoom")
                    ?.includeCustom(true)
                    ?.async { result, status ->
                        Log.i("WAITING", "getSpeakerStatus: $result")
                    }
            } catch (e: Exception){
                sendPubNubException(e)
            }

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
        FallbackManager.end()
    }


    private fun getLatestUserList() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            try {
//                throw Exception()

            pubnub.channelMembers.channel(liveRoomProperties?.channelName)
                ?.includeCustom(true)
                ?.async { result, status ->

                    extractUsersList(result, status)
                }
            }
            catch (e:SocketTimeoutException){
//                showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
                sendPubNubException(e)
                postDataToNetworkFlow(true)
                FallbackManager.getUsersList()
            }
            catch (e: Exception){
                FallbackManager.getUsersList()
                sendPubNubException(e)
            }

        }

    }

    fun postDataToNetworkFlow(isSlow:Boolean){
        jobs+= CoroutineScope(Dispatchers.IO).launch {
            delay(200)
            networkFlow.emit(isSlow)
        }

    }

    private fun extractUsersList(result: PNGetChannelMembersResult?, status: PNStatus) {

        try {
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
        } catch (e: Exception){

        }

    }

     fun refreshUsersList(uid: String, state: Any): LiveRoomUser? {
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

//        val data = msg["data"].asJsonObject
        val data = if (msg.has("data")) msg["data"].asJsonObject else msg["message"].asJsonObject

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

     fun postToSpeakersList(list: ArraySet<LiveRoomUser>) {
        Timber.d("post to speaker list => $list")
        val distinctedList = list.reversed().distinctBy { it.userId }.reversed().toSet()
        jobs += CoroutineScope(Dispatchers.IO).launch {
            _speakersList.emit(ArraySet(distinctedList))
        }
    }

     fun postToAudienceList(list: ArraySet<LiveRoomUser>) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            try{
                val distinctedList = list.reversed().distinctBy { it.userId }.reversed().toSet()
            _audienceList.emit(ArraySet(distinctedList))
            } catch (Ex:Exception){
                _audienceList.emit(arraySetOf())
            }
        }
    }

    fun postToSpeakerStatus(message: Message) {
        jobs+= CoroutineScope(Dispatchers.IO).launch {
            Log.i("MODERATORSTATUS", "postToSpeakerStatus: $message")
            moderatorStatus.emit(message)
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
        val eventId = System.currentTimeMillis().toString()
        val eventData = state.asJsonObject
        eventData.addProperty("event_id", eventId)
        jobs += CoroutineScope(Dispatchers.IO).launch() {
            try {
//                throw Exception()
                Log.i("MODERATORSTATUS", "sendCustomMessage: $channel")
                channel.let {
                    pubnub.publish()
                        .message(eventData)
                        ?.channel(it)
                        ?.async { result, status ->
                            Log.i("MODERATORSTATUS", "sendCustomMessage: $result && $status")
                        }
                }
            }
            catch (e:SocketTimeoutException){
                sendPubNubException(e)
                postDataToNetworkFlow(true)
//                showToast(AppObjectController.joshApplication.getString(R.string.internet_not_available_msz))
            }
            catch (e: Exception){
                e.printStackTrace()
                sendPubNubException(e)
            }
            sendEventToFallback(eventData, channel)
        }
    }

    private fun sendEventToFallback(eventData: JsonObject?, channel: String) {
//        if (channel != liveRoomProperties!!.channelName){
        eventData?.let {
            val d = JsonObject()
            d.add("message", eventData)
//            eventData.add("message", it)
            FallbackManager.sendEvent(d, channel)
        }
//        }
    }

    fun unSubscribePubNub() {
        pubnub.unsubscribeAll()
        endPubNub()
    }

    fun waitingUnsubscribe(){
        try{
            isJoinedPubnub.unsubscribeAll()
            waitingCallback?.let {
                isJoinedPubnub.removeListener(it)
            }
        }
        catch(ex:Exception){
        }
    }

    fun reconnectPubNub() {
        if (!reconnecting){
            reconnecting = true
            pubnub.reconnect()
            reconnecting = false
        }
    }

//    fun fullReconnect(){
//        if (!reconnecting) {
//            Log.d("Fallback", "reconnect: Pubnub Reconnecting - $reconnecting")
//                try{
//                        reconnecting = true
////                        pubnub?.removeListener(ca)
////                        pubnub?.unsubscribeAll()
//                        pubnub?.reconnect()
////                        pubnub?.addListener(listener)
////                        pubnub?.subscribe()
////                            ?.channels(listOf(Utils.uuid))
////                            ?.execute()
//                        CallAnalytics.addAnalytics(
//                            event = EventName.PUBNUB_LISTENER_RESTART,
//                            agoraCallId = PrefManager.getAgraCallId().toString(),
//                            agoraMentorId = PrefManager.getLocalUserAgoraId().toString()
//                        )
//                        isReconnecting = false
//                }
//                catch (e : Exception){
//                    if(e is CancellationException)
//                        throw e
//                    e.printStackTrace()
//                }
//        }
//
//    }

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
//        val data: JsonObject? = msg["data"].asJsonObject
        val data = if (msg.has("data")) msg["data"].asJsonObject else msg["message"].asJsonObject
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
        var data = msg
        if (msg.has("message"))
            data = msg["message"].asJsonObject
        data.get("id").asInt?.let { agoraId ->
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
                    bundle.putString(NOTIFICATION_NAME, data.get("short_name")?.asString)
                }
                message.data = bundle
                postToLiveEvent(message)
            }
        }
    }

     fun postToPubNubEvent(data: ConversationRoomPubNubEventBus) {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            Timber.d("ABC Event AUR EVENT ID HAI => ${data.eventId}")
            eventsMap[data.eventId] = data.eventId
            Timber.d("ABC Event AUR MAP HAI => ${eventsMap}")

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
                Timber.tag("Fallback").d("PUBNUB EVENT IS => $it")
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

    private fun sendPubNubException(e: Exception){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PubNubExceptionRepository().sendPubNubException(
                    PubNubExceptionRequest(
                        android_version = Build.VERSION.RELEASE.toDouble(),
                        app_version_code = BuildConfig.VERSION_CODE,
                        device_id = Utils.getDeviceId(),
                        stack_tree = e.stackTraceToString()
                    )
                )
            } catch (e: Exception){

            }
        }
    }

}