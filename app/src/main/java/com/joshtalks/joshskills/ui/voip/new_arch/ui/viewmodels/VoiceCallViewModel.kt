package com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels

import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.base.log.Feature
import com.joshtalks.joshskills.base.log.JoshLog
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.call.repository.WebrtcRepository
import com.joshtalks.joshskills.voip.constant.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val CONNECTING = 1
const val ONGOING = 2

private const val TAG = "VoiceCallViewModel"
val voipLog = JoshLog.getInstanceIfEnable(Feature.VOIP)

class VoiceCallViewModel : BaseViewModel() {
    private var isConnectionRequestSent = false
    lateinit var source: String
    private val repository = WebrtcRepository()
    private val mutex = Mutex(false)
    val callType = ObservableField("")
    val callStatus = ObservableInt(getCallStatus())
    val callData = HashMap<String,Any>()
    val uiState by lazy { repository.uiState }
    private val connectCallJob by lazy {
        viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutex.withLock {
                if (VoipPref.getVoipState() == IDLE && isConnectionRequestSent.not()) {
                    voipLog?.log("$callData")
                    repository.connectCall(callData)
                    isConnectionRequestSent = true
                }
            }
        }
    }

    init {
        listenRepositoryEvents()
    }

    private fun getCallStatus() : Int {
        val status = VoipPref.getVoipState()
        return if (status == CONNECTED) {
            ONGOING
        } else {
            CONNECTING
        }
    }

    private fun listenRepositoryEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeRepositoryEvents().collect { it ->
                message.copyFrom(it)
                Log.d(TAG, "listenRepositoryEvents: ")
                voipLog?.log("observeCallEvents =-> $it")
                val data = when (message.what) {
                    CALL_INITIATED_EVENT -> {
                        voipLog?.log("CALL_INITIATED_EVENT")
                        "Connecting"
                    }
                    CALL_CONNECTED_EVENT -> {
                        callStatus.set(ONGOING)
                        voipLog?.log("CALL_CONNECTED_EVENT")
                        "Timer"
                    }
                    RECONNECTING -> {
                        voipLog?.log("RECONNECTING")
                        "Reconnecting"
                    }
                    RECONNECTED -> {
                        voipLog?.log("RECONNECTED")
                        "Timer"
                    }
                    CALL_DISCONNECT_REQUEST, RECONNECTING_FAILED -> {
                        voipLog?.log("Call Disconnect")
                        showToast("Call Disconnect")
                        null
                    }
                    IPC_CONNECTION_ESTABLISHED -> {
                        connectCall()
                        null
                    }
                    else -> null
                }
                withContext(Dispatchers.Main) {
                    data?.let { uiState.currentState = it }
                    singleLiveEvent.value = message
                }
            }
        }
    }

    private fun connectCall() {
        connectCallJob.start()
    }

    fun disconnectCall(v: View) {
        voipLog?.log("Disconnect Call")
        disconnect()
    }

    fun disconnect() {
        Log.d(TAG, "disconnect: ")
        repository.disconnectCall()
    }

    fun boundService() {
        voipLog?.log("binding Service")
        repository.startService()
    }

    fun unboundService() {
        voipLog?.log("unbound Service")
        repository.stopService()
    }

    fun switchSpeaker(v: View) {
        VoipPref.currentUserSpeakerState(uiState.isSpeakerOn.not())
    }

    fun switchMic(v: View) {
        VoipPref.currentUserMuteState(uiState.isMute.not())
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared: ")
        super.onCleared()
        repository.clearRepository()
    }
}