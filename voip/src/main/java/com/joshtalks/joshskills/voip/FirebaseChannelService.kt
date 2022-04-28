package com.joshtalks.joshskills.voip

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import kotlin.Exception

private const val TAG = "FirebaseChannelService"
private const val PENDING = 0
private const val PROCESSED = 1

object FirebaseChannelService : EventChannel {
//    private val settings = FirebaseFirestoreSettings.Builder()
//        .setPersistenceEnabled(false)
//        .build()

    private val dataFlow by lazy {
        MutableSharedFlow<Communication>(replay = 0)
    }

    private val firestore by lazy {
        Firebase.firestore/*.apply { firestoreSettings = settings }*/
    }
    private val coroutineExceptionHandler = CoroutineExceptionHandler{_, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val networkDb by lazy {
        firestore.collection("p2p-testing")
            .document("${Utils.uuid}")
            //.document("testing-23")
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

    private val ioScope by lazy {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    }
    private val listener by lazy { FirebaseEventListener(ioScope) }
    
    override suspend fun initChannel() {
            observeFirestoreEvents()
            networkDb.addSnapshotListener(listener)
    }

    private fun observeFirestoreEvents() {
        ioScope.launch {
            try{
                listener.observerListener().collect { it ->
                    val timestamp = it.getEventTime()
                    dataFlow.emit(it)
                    timestamp?.let { processEvent(it) }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun emitEvent(event: OutgoingData) {
        throw IllegalOperationException("Firestore Channel Service not support sending message to client")
    }

    override fun observeChannelEvents(): SharedFlow<Communication> {
        return dataFlow
    }

    override fun reconnect() {
        //TODO "Not yet implemented"
    }
}

class FirebaseEventListener(val scope : CoroutineScope) : EventListener<DocumentSnapshot> {
    private val TAG = "FirebaseChannelService"

    private val dataFlow by lazy {
        MutableSharedFlow<Communication>(replay = 0)
    }

    fun observerListener() : SharedFlow<Communication> {
        return dataFlow
    }

    override fun onEvent(value: DocumentSnapshot?, error: FirebaseFirestoreException?) {
        scope.launch {
            try {
                if (value != null) {
                    Log.d(TAG, "onEvent: ${value.data} ... $value")
                    val data = value.data
                    if(data?.get("status").toString().toInt() == PROCESSED)
                        return@launch
                    val message = getMessage(data)
                    Log.d(TAG, "onEvent: $message")
                    // TODO: RED FLAG -- Must be removed
                    waitForValueToGetUpdated()
                    dataFlow.emit(message)
                } else {
                    Log.d(TAG, "onEvent: ERROR")
                    error?.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dataFlow.emit(Error())
            }
        }
    }

    private suspend fun waitForValueToGetUpdated() {
        Log.d(TAG, "waitForValueToGetUpdated: ")
        while (PrefManager.getLatestPubnubMessageTime() == 0L)
            delay(200)
    }

    private fun getMessage(messageMap : Map<String, Any?>?) : Communication {
        return when(messageMap?.get("type").toString().toInt()) {
            ServerConstants.CHANNEL -> Channel.fromMap(messageMap)
            ServerConstants.INCOMING_CALL -> IncomingCall.fromMap(messageMap)
            else -> Message.fromMap(messageMap)
        }
    }
}

class IllegalOperationException(message: String?) : Exception(message)