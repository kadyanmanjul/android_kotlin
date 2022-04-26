package com.joshtalks.joshskills.ui.call.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.call.data.local.VoipPrefListener
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.CallUIState
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val TAG = "WebrtcRepository"

class WebrtcRepository {
    private var mService: Messenger? = null
    private var handler: Messenger? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val repositoryHandler = RepositoryHandler(ioScope)
    private val repositoryToVMFlow = MutableSharedFlow<Message>(replay = 0)
    val uiState by lazy { CallUIState() }

    init {
        voipLog?.log("INIT .... ")
        Log.d(TAG, "INIT : ")
        observeCallEvents()
        try {
            val remoteServiceIntent =
                Intent(AppObjectController.joshApplication, CallingRemoteService::class.java)
            val apiHeader = ApiHeader(
                token = "JWT " + PrefManager.getStringValue(API_TOKEN),
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE.toString(),
                userAgent = "APP_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE.toString(),
                acceptLanguage = PrefManager.getStringValue(USER_LOCALE)
            )
            remoteServiceIntent.putExtra(INTENT_DATA_MENTOR_ID, Mentor.getInstance().getId())
            remoteServiceIntent.putExtra(INTENT_DATA_API_HEADER, apiHeader)
            AppObjectController.joshApplication.startService(remoteServiceIntent)
        } catch (e: Exception) {
            voipLog?.log("ERROR at INIT...")
            e.printStackTrace()
        }

        ioScope.launch {
            VoipPrefListener.observerVoipUserUIState().collect {
                setUserUIState()
            }
        }

        VoipPrefListener.observerStartTime().observeForever(::updateCallStartTime)

        ioScope.launch {
            VoipPrefListener.observerVoipUIState().collect { state ->
                Log.d(TAG, "listenUIState: $state")
                if (state.isOnHold) {
                    uiState.currentState = "Call on Hold"
                    voipLog?.log("HOLD")
                } else if (state.isRemoteUserMuted) {
                    uiState.currentState = "User Muted the Call"
                    voipLog?.log("Mute")
                } else {
                    if (VoipPref.getVoipState() == CONNECTED)
                        uiState.currentState = "Timer"
                }

                if (uiState.isSpeakerOn != state.isSpeakerOn) {
                    if (state.isSpeakerOn) {
                        uiState.isSpeakerOn = true
                        turnOnSpeaker()
                    } else {
                        uiState.isSpeakerOn = false
                        turnOffSpeaker()
                    }
                }

                if (uiState.isMute != state.isMute) {
                    if (state.isMute) {
                        uiState.isMute = true
                        muteCall()
                    } else {
                        uiState.isMute = false
                        unmuteCall()
                    }
                }
            }
        }
    }

    fun updateCallStartTime(startTime : Long) {
        uiState.startTime = startTime
    }

    private fun setUserUIState() {
        val name = VoipPref.getCallerName()
        if(name.isNotEmpty()) {
            uiState.name = name
            uiState.profileImage = VoipPref.getProfileImage()
            uiState.topic = VoipPref.getTopicName()
            uiState.type = VoipPref.getCallType()
            uiState.title = when(VoipPref.getCallType()) {
                PEER_TO_PEER -> "Practice with Partner"
                FPP -> "Favorite Practice Partner"
                GROUP -> "Group Call"
                else -> ""
            }
        }
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = Messenger(service)
            handler = Messenger(repositoryHandler)
            // After Connection must send handler to service to listen for messages
            val msg = Message().apply {
                what = IPC_CONNECTION_ESTABLISHED
                replyTo = handler
            }
            voipLog?.log("Connection Establish")
            sendMessageToRemoteService(msg)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    fun observeRepositoryEvents(): SharedFlow<Message> {
        return repositoryToVMFlow
    }

    private fun observeCallEvents() {
        ioScope.launch {
            repositoryHandler.observerFlow().collect {
                repositoryToVMFlow.emit(it)
            }
        }
    }

    fun startService() {
        val remoteServiceIntent =
            Intent(AppObjectController.joshApplication, CallingRemoteService::class.java)
        AppObjectController.joshApplication.bindService(
            remoteServiceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun stopService() {
        AppObjectController.joshApplication.unbindService(serviceConnection)
    }

    fun connectCall(callData: HashMap<String, Any>) {
        val msg = Message().apply {
            what = CALL_CONNECT_REQUEST
            obj = Bundle().apply {
                putSerializable(INTENT_DATA_CONNECT_CALL, callData)
            }
        }
        voipLog?.log("Call Connect --> $msg Call Data --> $callData")
        sendMessageToRemoteService(msg)
    }

    fun disconnectCall() {
        Log.d(TAG, "disconnectCall: ")
        voipLog?.log("Disconnect call")
        val msg = Message().apply {
            what = CALL_DISCONNECT_REQUEST
        }
        sendMessageToRemoteService(msg)
    }

    fun muteCall() {
        voipLog?.log("Disconnect call")
        val msg = Message().apply {
            what = MUTE
        }
        sendMessageToRemoteService(msg)
    }

    fun turnOnSpeaker() {
        val msg = Message().apply {
            what = SPEAKER_ON_REQUEST
        }
        sendMessageToRemoteService(msg)
    }

    fun turnOffSpeaker() {
        val msg = Message().apply {
            what = SPEAKER_OFF_REQUEST
        }
        sendMessageToRemoteService(msg)
    }

    fun unmuteCall() {
        voipLog?.log("Disconnect call")
        val msg = Message().apply {
            what = UNMUTE
        }
        sendMessageToRemoteService(msg)
    }

    private fun sendMessageToRemoteService(msg: Message) {
        try {
            val data = Message()
            data.copyFrom(msg)
            voipLog?.log("$data  $mService")
            mService?.send(data)
            when (data.what) {
                // Returning back these two event to View Model so that they can take action
                IPC_CONNECTION_ESTABLISHED, CALL_DISCONNECT_REQUEST, RECONNECTING_FAILED -> {
                    ioScope.launch {
                        repositoryToVMFlow.emit(data)
                    }
                }
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
            voipLog?.log("Service Closed")
        }
    }

    fun clearRepository() {
        VoipPrefListener.observerStartTime().removeObserver(::updateCallStartTime)
        ioScope.cancel()
    }
}