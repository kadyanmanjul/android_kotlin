package com.joshtalks.joshskills.ui.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.INTENT_DATA_API_HEADER
import com.joshtalks.joshskills.base.constants.INTENT_DATA_CONNECT_CALL
import com.joshtalks.joshskills.base.constants.INTENT_DATA_MENTOR_ID
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.voip.constant.CALL_CONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECT_REQUEST
import com.joshtalks.joshskills.voip.constant.IPC_CONNECTION_ESTABLISHED
import com.joshtalks.joshskills.voip.data.CallingRemoteService
import com.joshtalks.joshskills.voip.voipLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class WebrtcRepository() {
    private var mService : Messenger? = null
    private var handler : Messenger? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val repositoryHandler = RepositoryHandler(scope)
    private val repositoryToVMFlow = MutableSharedFlow<Int>(replay = 0)

    init {
        voipLog?.log("INIT .... ")
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
        } catch (e:Exception) {
            voipLog?.log("ERROR at INIT...")
            e.printStackTrace()
        }
    }

    fun getRepositoryEvents() : SharedFlow<Int> {
        return repositoryToVMFlow
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

    fun observeCallEvents() : SharedFlow<Message> {
        voipLog?.log("observeCallEvents")
        return repositoryHandler.observerFlow()
    }

    fun startService() {
        val remoteServiceIntent = Intent(AppObjectController.joshApplication, CallingRemoteService::class.java)
        AppObjectController.joshApplication.bindService(remoteServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun stopService() {
        AppObjectController.joshApplication.unbindService(serviceConnection)
    }

    fun connectCall(callData : HashMap<String, Any>) {
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
        voipLog?.log("Disconnect call")
        val msg = Message().apply {
            what = CALL_DISCONNECT_REQUEST
        }
        sendMessageToRemoteService(msg)
    }

    private fun sendMessageToRemoteService(msg : Message) {
        try {
            val data = Message()
            data.copyFrom(msg)
            voipLog?.log("$data  $mService")
            mService?.send(data)
            if (data.what == IPC_CONNECTION_ESTABLISHED)
                scope.launch {
                    repositoryToVMFlow.emit(IPC_CONNECTION_ESTABLISHED)
                }
        } catch (e : RemoteException) {
            e.printStackTrace()
            voipLog?.log("Remote Closed")
        }
    }
}