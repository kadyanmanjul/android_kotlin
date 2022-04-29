package com.joshtalks.joshskills.voip.communication

import android.util.Log
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.constants.ServerConstants
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.data.api.CallDisconnectRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.data.local.DisconnectCallEntity
import com.joshtalks.joshskills.voip.data.local.SYNCED
import com.joshtalks.joshskills.voip.data.local.VoipDatabase
import com.joshtalks.joshskills.voip.voipLog
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.PubNubException
import com.pubnub.api.enums.PNReconnectionPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "PubNubChannelService"
enum class PubnubState {
    CONNECTED,
    RECONNECTED,
    DISCONNECTED
}

class PubNubChannelService(val scope: CoroutineScope) : EventChannel {
    private val database by lazy {
        VoipDatabase.getDatabase(Utils.context!!.applicationContext)
    }
    private val callApiService by lazy {
        VoipNetwork.getVoipApi()
    }

    private val listener by lazy {
        PubNubSubscriber(scope)
    }

    var isReconnecting = false

    private val mutex = Mutex(false)

    private var pubnub: PubNub? = null
    //private val channelName = Utils.uuid

    private val eventFlow = MutableSharedFlow<Communication>(replay = 0)

    private val config by lazy {
        PNConfiguration().apply {
            //logVerbosity = PNLogVerbosity.BODY
        }
    }

    init {
        config.publishKey = BuildConfig.PUBNUB_PUB_P2P_KEY
        config.subscribeKey = BuildConfig.PUBNUB_SUB_P2P_KEY
        config.reconnectionPolicy = PNReconnectionPolicy.LINEAR
        config.uuid = Utils.uuid
        //config.httpLoggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        //config.uuid = "Mentor.getInstance().getId()"
        pubnub = PubNub(config)
        pubnub?.addListener(listener)
        pubnub?.subscribe()
            ?.channels(listOf(Utils.uuid))
            ?.execute()
        observeIncomingMessage()
    }

    override fun reconnect() {
        Log.d(TAG, "reconnect: Pubnub")
        if (isReconnecting.not()) {
            Log.d(TAG, "reconnect: Pubnub .....")
            scope.launch {
                mutex.withLock {
                    isReconnecting = true
                    pubnub?.removeListener(listener)
                    pubnub?.unsubscribeAll()
                    pubnub?.reconnect()
                    pubnub?.addListener(listener)
                    pubnub?.subscribe()
                        ?.channels(listOf(Utils.uuid))
                        ?.execute()
                    isReconnecting = false
                }
            }
        }
    }

    override fun emitEvent(event: OutgoingData) {
        scope.launch {
            try {
                val message = when (event) {
                    is NetworkActionData -> event as NetworkAction
                    is UIState -> event as UI
                    is UserActionData -> event as UserAction
                    is Timeout -> event
                }
                Log.d(TAG, "emitEvent: Sending Message .... $message")
                event.captureDisconnectEvent()
                pubnub?.publish()
                    ?.channel(event.getAddress())
                    ?.meta(getMeta(event))
                    ?.message(message)
                    ?.ttl(0)
                    ?.usePOST(true)
                    ?.sync()
                Log.d(TAG, "emitEvent: Message Sent .... $message")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun OutgoingData.captureDisconnectEvent() {
        if(this.getType() == ServerConstants.DISCONNECTED) {
            try {
                val data = this as NetworkAction
                scope.launch {
                    data.insertIntoDb()
                    callApiService.disconnectCall(data.toRequest())
                    data.insertIntoDb(status = SYNCED)
                    database.getDisconnectCallDao().delete()
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    fun NetworkAction.toRequest() : CallDisconnectRequest {
        return CallDisconnectRequest(
            duration = this.getDuration(),
            response = "DISCONNECT",
            mentorId = this.getAddress(),
            channelName = this.getChannelName()
        )
    }

    private suspend fun NetworkAction.insertIntoDb(status : Int = 0) {
        database.getDisconnectCallDao().insertDisconnectedData(
            DisconnectCallEntity(
                channelName = this.getChannelName(),
                mentorId = this.getAddress(),
                duration = this.getDuration(),
                status = status
            )
        )
    }

    private fun getMeta(event : OutgoingData) = if(Utils.uuid == event.getAddress()) null else event.getType()

    override fun observeChannelEvents(): SharedFlow<Communication> {
        voipLog?.log("observeChannelEvents: $pubnub")
        return eventFlow
    }

    override fun observeChannelState(): SharedFlow<PubnubState> {
        return listener.observeChannelState()
    }

    private fun observeIncomingMessage() {
        Log.d(TAG, "observeIncomingMessage: ${listener.hashCode()}")
        scope.launch {
            listener.observeMessages().collect {
                //if (state == State.ACTIVE)
                Log.d(TAG, "observeIncomingMessage: $it")
                when (it) {
                    is MessageData -> eventFlow.emit(it)
                    is ChannelData -> eventFlow.emit(it)
                    is IncomingCall -> eventFlow.emit(it)
                    is UI -> eventFlow.emit(it)
                    is Error -> eventFlow.emit(it)
                }
            }
        }
    }

    override fun onDestroy() {
        pubnub?.removeListener(listener)
        pubnub?.unsubscribeAll()
        pubnub?.destroy()
        pubnub = null
    }
}