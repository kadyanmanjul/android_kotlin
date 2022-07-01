package com.joshtalks.badebhaiya.liveroom

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.fallback.FallbackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

object WaitingRoomManager {
    const val TAG = "WaitingRoomManager"
    const val WAITING_ROOM = "waiting_room"
    const val HAS_SPEAKER_JOINED = "has_speaker_joined"

    private val jobs = mutableListOf<Job>()

    val hasSpeakerJoined = MutableSharedFlow<Boolean>()

    private var documentRef: ListenerRegistration? = null

    fun sendModeratorJoinedEvent() {

        if (!PubNubManager.isModerator()) {
            return
        }

        val hasSpeakerJoined = mapOf(
            HAS_SPEAKER_JOINED to true
        )

        Firebase.firestore
            .collection(WAITING_ROOM)
            .document(FallbackManager.getRoomId())
            .collection(WAITING_ROOM)
            .document(WAITING_ROOM)
            .set(hasSpeakerJoined)
            .addOnSuccessListener {
                Log.d(TAG, "path: ${FallbackManager.getRoomId()}")

                Timber.tag(TAG).d("MODERATOR JOINED EVENT SENT TO FIRESTORE")
            }
            .addOnFailureListener {
                Timber.tag(TAG)
                    .d("MODERATOR JOINED EVENT SENT TO FIRESTORE FAILED AND EXCEPTION => $it")
            }
    }

    fun observeIfModeratorJoined() {
        documentRef = Firebase.firestore
            .collection(WAITING_ROOM)
            .document(FallbackManager.getRoomId())
            .collection(WAITING_ROOM)
            .document(WAITING_ROOM)
//            .collection("yoyo")
//            .document("yoyo")
//            .collection("yoyo")
//            .document("yoyo")
            .addSnapshotListener { value, error ->
                try {
                    Log.d(TAG, "path: ${FallbackManager.getRoomId()}")

                    Log.d(TAG, "observeIfModeratorJoined: ${value?.data}")
                    if (error == null) {
                        value?.let {
                            it.getBoolean(HAS_SPEAKER_JOINED)?.let { speakerJoined ->
                                if (speakerJoined) {
                                    postToSpeakerJoinedFlow()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
    }

    private fun postToSpeakerJoinedFlow() {
        jobs += CoroutineScope(Dispatchers.IO).launch {
            hasSpeakerJoined.emit(true)
        }
    }

    fun finish() {
        jobs.forEach {
            it.cancel()
        }
        jobs.clear()
        documentRef?.remove()
    }

}