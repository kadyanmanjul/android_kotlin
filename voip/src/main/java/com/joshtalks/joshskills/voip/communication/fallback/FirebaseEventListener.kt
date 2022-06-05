package com.joshtalks.joshskills.voip.communication.fallback

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.gson.Gson
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.data.local.PrefManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class FirebaseEventListener(val scope : CoroutineScope) : EventListener<DocumentSnapshot> {
    private val TAG = "FirebaseChannelService"

    private val dataFlow by lazy { MutableSharedFlow<Communication>(replay = 0) }

    fun observerListener() : SharedFlow<Communication> { return dataFlow }

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
                if(e is CancellationException)
                    throw e
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
            ServerConstants.GROUP_INCOMING_CALL -> GroupIncomingCall.fromMap(messageMap)
            ServerConstants.UI_STATE_UPDATED, ServerConstants.ACK_UI_STATE_UPDATED -> UI.fromMap(messageMap)
            else -> Message.fromMap(messageMap)
        }
    }
}