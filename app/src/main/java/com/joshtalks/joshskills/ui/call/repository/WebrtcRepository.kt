package com.joshtalks.joshskills.ui.call.repository

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.call.repository.RepositoryConstants.*
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.voipLog
import com.joshtalks.joshskills.voip.constant.*
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.getApiHeader
import com.joshtalks.joshskills.voip.getMentorId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private const val TAG = "WebrtcRepository"

class WebrtcRepository(scope : CoroutineScope) {
    private var mService: CallingRemoteService? = null
    private val repositoryToVMFlow = MutableSharedFlow<RepositoryConstants>(replay = 0)

    init {
        voipLog?.log("INIT .... ")
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

    fun getVoipState() = mService?.currentState

    fun observeRepositoryEvents(): SharedFlow<RepositoryConstants> {
        return repositoryToVMFlow
    }

    fun startService(activity : Activity) {
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
        with(activity) {
            unbindService(serviceConnection)
        }
    }

    fun connectCall(callData: HashMap<String, Any>) {
        Log.d(TAG, "connectCall: ")
        if(mService != null && (mService?.currentState == IDLE || mService?.currentState == LEAVING))
            mService?.connectCall(callData)
    }

    fun disconnectCall() {
        mService?.disconnectCall()
    }

    fun muteCall() {
        mService?.changeMicState(false)
    }

    fun turnOnSpeaker() {
        mService?.changeSpeakerState(true)
    }

    fun turnOffSpeaker() {
        mService?.changeSpeakerState(false)
    }

    fun unmuteCall() {
        mService?.changeMicState(true)
    }

    fun backPress() {

    }

    fun clearRepository() {
        //ioScope.cancel()
    }
}

enum class RepositoryConstants {
    CONNECTION_ESTABLISHED
}