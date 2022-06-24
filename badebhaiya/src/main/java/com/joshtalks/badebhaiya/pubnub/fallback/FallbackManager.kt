package com.joshtalks.badebhaiya.pubnub.fallback

import android.util.Log
import androidx.collection.ArraySet
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import com.joshtalks.badebhaiya.pubnub.*
import com.joshtalks.badebhaiya.utils.toHashMap
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.member.PNGetChannelMembersResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import timber.log.Timber

/**
This object is responsible to manage pubnub fallback which runs on Firebase Could Firestore.
 */

object FallbackManager {

    private const val TIMESTAMP = "timestamp"
    private const val COLLECTION_NAME = "live_room"
    private const val EVENT_ID = "event_id"
    private const val TAG = "FallbackManager"
    private const val LIVE_ROOM = "LIVE_ROOM"
    private const val CHANNELS = "CHANNELS"
    private const val USER_LIST = "USER_LIST"
    private const val user_list = "user_list"

    private var globalChannelListener: ListenerRegistration? = null
    private var privateChannelListener: ListenerRegistration? = null

    private val jobs = mutableListOf<Job>()

    fun start() {
        startGlobalChannel()
        startPrivateChannel()
    }

    private fun startGlobalChannel() {
        globalChannelListener = Firebase.firestore
            .collection(LIVE_ROOM)
            .document(getRoomId())
            .collection(CHANNELS)
            .document(PubNubManager.getLiveRoomProperties().channelName)
            .addSnapshotListener { value, error ->
                Timber.tag(TAG)
                    .d("EVENT RECIEVED FROM GLOBAL CHANNEL AND ERROR => $error and VALUE => $value")

                if (error == null) {
                    value?.let {
                        processEvent(it)
                    }
                }
            }
    }

    private fun startPrivateChannel() {
        privateChannelListener = Firebase.firestore
            .collection(LIVE_ROOM)
            .document(getRoomId())
            .collection(CHANNELS)
            .document(PubNubManager.getLiveRoomProperties().agoraUid.toString())
            .addSnapshotListener { value, error ->
                Timber.tag(TAG).d("EVENT RECIEVED FROM PRIVATE CHANNEL")

                if (error == null) {
                    value?.let {
                        processEvent(it)
                    }
                }
            }
    }

    private fun processEvent(documentSnapshot: DocumentSnapshot) {
        if (documentSnapshot.exists()) {
            try {
                val doc = documentSnapshot["message"] as HashMap<*, *>
                doc["event_id"].toString().toLong().let {
                    if (it > PubNubManager.roomJoiningTime) {

                        if (!checkEventExist(it)) {
                            Timber.tag(TAG).d("NO EVENT DOESN'T EXISTS")
                            sendEventToFlow(documentSnapshot)
                        } else {
                            Timber.tag(TAG).d("YES EVENT EXISTS")
                        }

                    }
                }
            } catch (e: Exception){
                Timber.tag(TAG).d("EVENT ID DOES NOT EXIST")
            }

        }
    }

    private fun sendEventToFlow(documentSnapshot: DocumentSnapshot) {
        Timber.tag(TAG)
            .d("FIRESTORE DATA IS => $documentSnapshot and DATA IS => ${documentSnapshot.data}")
        try {
            documentSnapshot.data?.let { data ->
                val doc = documentSnapshot["message"] as HashMap<*, *>
                PubNubManager.postToPubNubEvent(
                    ConversationRoomPubNubEventBus(
                        eventId = doc[EVENT_ID].toString().toLong(),
                        action = PubNubEvent.valueOf(doc["action"].toString()),
//                    data = JsonParser.parseString(Gson().toJson(documentSnapshot.data)).asJsonObject
                        data = getData(documentSnapshot, doc)
                    )
                )

                PubNubManager.reconnectPubNub()

            }
        } catch (e: Exception){

        }

    }

    private fun getData(documentSnapshot: DocumentSnapshot, dataMap: HashMap<*, *>): JsonObject {
        return when (dataMap["action"]) {
            "JOIN_ROOM", "END_ROOM", "LEAVE_ROOM" -> JsonParser.parseString(
                Gson().toJson(
                    documentSnapshot.data
                )
            ).asJsonObject
            else -> JsonParser.parseString(Gson().toJson(dataMap)).asJsonObject
        }
    }

    private suspend fun checkIfEventExists(timestamp: Long): Boolean {
        Timber.tag(TAG).d("EVENT EXISTS CALLED")

        var eventExists = false
        PubNubData.pubNubEvents
            .filter { it.eventId == timestamp }
            .onEmpty {
                eventExists = false
            }.collect {
                eventExists = it.eventId == timestamp
                return@collect
            }
        Timber.tag(TAG).d("EVENT EXISTS => $eventExists")
        return eventExists
    }

    private fun checkEventExist(timestamp: Long): Boolean {
        return PubNubData.eventsMap.containsKey(timestamp)
    }

    fun sendEvent(eventData: JsonObject, channel: String) {

        Firebase.firestore
            .collection(LIVE_ROOM)
            .document(getRoomId())
            .collection(CHANNELS)
            .document(channel)
            .set(eventData.toHashMap())
            .addOnSuccessListener {
                Timber.tag(TAG).d("EVENT SENT TO FIRESTORE")
            }
            .addOnFailureListener {
                Timber.tag(TAG).d("EVENT SENT TO FIRESTORE FAILED AND EXCEPTION => $it")
            }
    }

    private fun getRoomId(): String {
        return PubNubManager.getLiveRoomProperties().roomId.toString()
    }

    fun end() {
        jobs.forEach {
            it.cancel()
        }
        jobs.clear()
        globalChannelListener?.remove()
        privateChannelListener?.remove()

    }

    fun getUsersList() {
        Firebase.firestore
            .collection(LIVE_ROOM)
            .document(getRoomId())
            .collection(USER_LIST)
            .document(user_list)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val usersList = it.result.toObject(PubNubFallbackUser::class.java)
                    val usersJson = JsonParser.parseString(Gson().toJson(it.result.data))
                    Log.d(TAG, "getUsersList: ${it.result.data}")
                    Log.d(TAG, "getUsersList after conversion: ${usersList}")
                    Log.d(TAG, "getUsersList after conversion in json: ${usersJson}")
                    extractUsersList(usersJson.asJsonObject)

                }
            }
    }

    private fun extractUsersList(result: JsonObject) {

        try {
            val tempSpeakerList = mutableListOf<LiveRoomUser>()
            val tempAudienceList = mutableListOf<LiveRoomUser>()
            result["user_list"].asJsonArray.forEach {
                val uid = it.asJsonObject["uuid"].asJsonObject["id"].asString
                val custom = it.asJsonObject["custom"]

                Log.d("lvroom", "getLatestUserList() called with: memberList = $it ")
                PubNubManager.refreshUsersList(uid, custom)?.let { user ->

                    if (user.isSpeaker == true) {
//                    Timber.d("Memebers List and speaker h=> ${it.uuid}")

                        tempSpeakerList.add(user)
                    } else {
//                    Timber.d("Memebers List and audience h=> ${it.uuid}")

                        tempAudienceList.add(user)
                    }
                }
            }
            // post to a shared flow instead of live data
            Timber.d("THIS IS WITH MEMBERS LIST SPEAKER => $tempSpeakerList AND AUDIENCE => $tempAudienceList")
            PubNubManager.postToSpeakersList(tempSpeakerList.toList())
            PubNubManager.postToAudienceList(tempAudienceList.toList())
        } catch (e: Exception){

        }
    }


}

fun Custom.toJsonElement(): JsonElement {
    return JsonParser.parseString(Gson().toJson(this))
}