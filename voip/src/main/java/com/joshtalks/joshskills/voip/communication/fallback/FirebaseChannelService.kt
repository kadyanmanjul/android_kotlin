package com.joshtalks.joshskills.voip.communication.fallback

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubnubState
import com.joshtalks.joshskills.voip.communication.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlin.Exception

private const val TAG = "FirebaseChannelService"
const val PENDING = 0
const val PROCESSED = 1

class FirebaseChannelService(val scope: CoroutineScope) : EventChannel {
    private val settings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(false)
        .build()

    private val dataFlow by lazy {
        MutableSharedFlow<Communication>(replay = 0)
    }

    private val firestore by lazy {
        Firebase.firestore.apply { firestoreSettings = settings }
    }

    private val networkDb by lazy {
        firestore.collection("p2p-testing")
            .document("${Utils.uuid}")
    }

    private val dbListenerRef by lazy {
        networkDb.addSnapshotListener(listener)
    }

    private val listener by lazy { FirebaseEventListener(scope) }

    init {
        observeFirestoreEvents()
        dbListenerRef
    }

    private fun processEvent(timestamp : Long) {
        firestore.runTransaction { transaction ->
            Log.d(TAG, "processEvent: $timestamp")
            val snapshot = transaction.get(networkDb)
            if(snapshot.getString("timetoken")?.toLong() == timestamp)
                transaction.update(networkDb, "status", PROCESSED)
        }.addOnSuccessListener {
            Log.d(TAG, "processEvent: Sucess")
        }.addOnFailureListener {
            Log.d(TAG, "processEvent: FAILED")
            it.printStackTrace()
        }
    }

    private fun observeFirestoreEvents() {
        scope.launch {
            try{
                listener.observerListener().collect { it ->
                    try{
                        val timestamp = it.getEventTime()
                        dataFlow.emit(it)
                        timestamp?.let { processEvent(it) }
                    }
                    catch (e : Exception){
                        if(e is CancellationException)
                            throw e
                        e.printStackTrace()
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
                if(e is CancellationException)
                    throw e
            }
        }
    }

    override fun emitEvent(event: OutgoingData) {
        throw IllegalOperationException("Firestore Channel Service not support sending message to client")
    }

    override fun observeChannelEvents(): SharedFlow<Communication> {
        return dataFlow
    }

    // TODO: Ignore
    override fun observeChannelState(): SharedFlow<PubnubState> {
        return MutableSharedFlow()
    }

    override fun reconnect() {
        //TODO "Not yet implemented"
    }

    override fun onDestroy() {
        try {
            dbListenerRef.remove()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }
}

class IllegalOperationException(message: String?) : Exception(message)