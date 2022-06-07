package com.joshtalks.joshskills.ui.call.repository

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.data.local.PrefManager
import com.joshtalks.joshskills.voip.getApiHeader
import com.joshtalks.joshskills.voip.getMentorId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val TAG = "WebrtcRepository"

class WebrtcRepository(scope : CoroutineScope) {
    private var mService: CallingRemoteService? = null
    private val repositoryToVMFlow = MutableSharedFlow<RepositoryConstants>(replay = 0)

    init {
        Log.d(TAG, "INIT : ")
        try {
            val remoteServiceIntent =
                Intent(com.joshtalks.joshskills.voip.Utils.context, CallingRemoteService::class.java)
            val apiHeader = com.joshtalks.joshskills.voip.Utils.context?.getApiHeader()
            remoteServiceIntent.putExtra(INTENT_DATA_MENTOR_ID, com.joshtalks.joshskills.voip.Utils.context?.getMentorId())
            remoteServiceIntent.putExtra(INTENT_DATA_API_HEADER, apiHeader)
            com.joshtalks.joshskills.voip.Utils.context?.startService(remoteServiceIntent)
        } catch (e: Exception) {
            voipLog?.log("ERROR at INIT...")
            e.printStackTrace()
        }
    }

    // It will be used for UI only no action will be taken on the basis of this
    fun observeUserDetails() = mService?.getUserDetails()

    fun observerVoipEvents() = mService?.getEvents()

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService =  (service as CallingRemoteService.RemoteServiceBinder).getService()
            scope.launch {
                Log.d(TAG, "onServiceConnected: ")
                repositoryToVMFlow.emit(CONNECTION_ESTABLISHED)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    fun observeRepositoryEvents(): SharedFlow<RepositoryConstants> {
        return repositoryToVMFlow
    }

    fun startService(activity : Activity) {
        Log.d(TAG, "startService: ")
        with(activity) {
            val remoteServiceIntent =
                Intent(this, CallingRemoteService::class.java)
            bindService(
                remoteServiceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    fun stopService(activity: Activity) {
        Log.d(TAG, "stopService: ")
        with(activity) {
            unbindService(serviceConnection)
        }
    }

    fun connectCall(callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: - $callData")
        if(mService != null && PrefManager.getVoipState() == State.IDLE)
            mService?.connectCall(callData)
    }

    fun disconnectCall() {
        Log.d(TAG, "disconnectCall: ")
        mService?.disconnectCall()
    }

    fun muteCall() {
        Log.d(TAG, "muteCall: ")
        mService?.changeMicState(false)
    }

    fun startCallRecording() {
        Log.d(TAG, "startCallRecording: ")
        mService?.startRecording()
    }

    fun stopCallRecording() {
        Log.d(TAG, "stopCallRecording: ")
        mService?.stopRecording()
    }

    fun turnOnSpeaker() {
        Log.d(TAG, "turnOnSpeaker: ")
        mService?.changeSpeakerState(true)
    }

    fun turnOffSpeaker() {
        Log.d(TAG, "turnOffSpeaker: ")
        mService?.changeSpeakerState(false)
    }

    fun unmuteCall() {
        Log.d(TAG, "unmuteCall: ")
        mService?.changeMicState(true)
    }

    fun backPress() {
        Log.d(TAG, "backPress: ")
        mService?.backPress()
    }

    fun clearRepository() {
        //ioScope.cancel()
    }

    fun getNewTopicImage() {
        mService?.changeTopicImage()
    }
}

enum class RepositoryConstants {
    CONNECTION_ESTABLISHED
}