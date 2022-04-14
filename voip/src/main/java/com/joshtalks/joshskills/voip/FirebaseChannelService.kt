package com.joshtalks.joshskills.voip

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.Exception

object FirebaseChannelService : EventChannel {
    private val firestore by lazy {
        Firebase.firestore
    }
    private val coroutineExceptionHandler = CoroutineExceptionHandler{_, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }
    private val networkDb by lazy {
        firestore.collection("p2p-testing")
            .document("${Utils.uuid}")
    }
    private val ioScope by lazy {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
    }
    private val listener by lazy {
        FirebaseEventListener(ioScope)
    }
    
    override suspend fun initChannel() {
            networkDb.addSnapshotListener(listener)
    }

    override fun emitEvent(event: OutgoingData) {
        throw IllegalOperationException("Firestore Channel Service not support sending message to client")
    }

    override fun observeChannelEvents(): SharedFlow<Communication> {
        return listener.observerListener()
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
                    val message = getMessage(value.data)
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

    private fun getMessage(messageMap : Map<String, Any?>?) : Communication {
        return when(messageMap?.get("type")) {
            ServerConstants.CHANNEL -> Channel.fromMap(messageMap)
            ServerConstants.INCOMING_CALL -> IncomingCall.fromMap(messageMap)
            else -> Message.fromMap(messageMap)
        }
    }
}

class IllegalOperationException(message: String?) : Exception(message)