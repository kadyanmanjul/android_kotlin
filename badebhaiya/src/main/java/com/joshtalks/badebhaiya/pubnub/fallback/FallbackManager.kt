package com.joshtalks.badebhaiya.pubnub.fallback

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import com.joshtalks.badebhaiya.pubnub.PubNubData
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.pubnub.api.PubNub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
This object is responsible to manage pubnub fallback which runs on Firebase Could Firestore.
 */

object FallbackManager {

    private const val TIMESTAMP = "timestamp"
    private const val COLLECTION_NAME = "live_room"

    private var globalChannelListener: ListenerRegistration? = null
    private var privateChannelListener: ListenerRegistration? = null

    private val jobs = mutableListOf<Job>()

    fun start() {
        startGlobalChannel()
        startPrivateChannel()
    }

    private fun startGlobalChannel() {
        globalChannelListener = Firebase.firestore
            .collection(COLLECTION_NAME)
            .document(PubNubManager.getLiveRoomProperties().channelName)
            .addSnapshotListener { value, error ->
                if (error == null) {
                    value?.let {
                        processEvent(it)
                    }
                }
            }
    }

    private fun startPrivateChannel() {
        privateChannelListener = Firebase.firestore
            .collection(COLLECTION_NAME)
            .document(PubNubManager.getLiveRoomProperties().agoraUid.toString())
            .addSnapshotListener { value, error ->
                if (error == null) {
                    value?.let {
                        processEvent(it)
                    }
                }
            }
    }

    private fun processEvent(documentSnapshot: DocumentSnapshot) {
        if (documentSnapshot.exists()) {
            documentSnapshot.getLong(TIMESTAMP)?.let {
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

            documentSnapshot.getLong(TIMESTAMP)?.let { timestamp ->
                PubNubManager.postToPubNubEvent(
                    ConversationRoomPubNubEventBus(
                        eventId = timestamp,
                        action = PubNubEvent.valueOf(documentSnapshot.getString("kjasj")!!), // TODO: Change key for event.
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

    fun end() {
        jobs.forEach {
            it.cancel()
        }
        jobs.clear()
        globalChannelListener?.remove()
        privateChannelListener?.remove()
    }

    fun sendEvent() {

    }
}