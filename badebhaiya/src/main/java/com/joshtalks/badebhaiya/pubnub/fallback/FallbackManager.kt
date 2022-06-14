package com.joshtalks.badebhaiya.pubnub.fallback

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.utils.toHashMap
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

    private var globalChannelListener: ListenerRegistration? = null
    private var privateChannelListener: ListenerRegistration? = null

    private val jobs = mutableListOf<Job>()

    fun start() {
        startGlobalChannel()
        startPrivateChannel()
    }

    private fun startGlobalChannel() {
        globalChannelListener = Firebase.firestore
            .collection(getRoomId())
            .document(PubNubManager.getLiveRoomProperties().channelName)
            .addSnapshotListener { value, error ->
                Timber.tag(TAG).d("EVENT RECIEVED FROM GLOBAL CHANNEL AND ERROR => $error and VALUE => $value")

                if (error == null) {
                    value?.let {
                        processEvent(it)
                    }
                }
            }
    }

    private fun startPrivateChannel() {
        privateChannelListener = Firebase.firestore
            .collection(getRoomId())
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
            documentSnapshot.getLong(EVENT_ID)?.let {
                jobs += CoroutineScope(Dispatchers.IO).launch {
                    if (checkIfEventExists(it)) {
                        sendEventToFlow(documentSnapshot)
                    }
                }
            }
        }
    }

    private fun sendEventToFlow(documentSnapshot: DocumentSnapshot) {
        documentSnapshot.data?.let { data ->
            documentSnapshot.getLong(EVENT_ID)?.let { timestamp ->
                PubNubManager.postToPubNubEvent(
                    ConversationRoomPubNubEventBus(
                        eventId = timestamp,
                        action = PubNubEvent.valueOf(documentSnapshot.getString("data")!!),
                        data = JsonObject().getAsJsonObject(data.toString())
                    )
                )
            }
        }

    }

    private suspend fun checkIfEventExists(timestamp: Long): Boolean {
        var eventExists = false
        PubNubData.pubNubEvents
            .filter { it.eventId == timestamp }
            .onEmpty {
                eventExists = false
            }.collect {
                eventExists = it.eventId == timestamp
            }
        return eventExists
    }

    fun sendEvent(eventData: JsonObject?, channel: String) {

        Firebase.firestore
            .collection(getRoomId())
            .document(channel)
            .set(eventData.toHashMap())
            .addOnSuccessListener {
                Timber.tag(TAG).d("EVENT SENT TO FIRESTORE")
            }
            .addOnFailureListener {
                Timber.tag(TAG).d("EVENT SENT TO FIRESTORE FAILED AND EXCEPTION => $it")
            }
    }

    private fun getRoomId(): String{
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


}