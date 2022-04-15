package com.joshtalks.joshskills.voip.communication

import android.util.Log
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.communication.model.*
import com.joshtalks.joshskills.voip.voipLog
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.enums.PNReconnectionPolicy
import java.sql.Time
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.annotations.NotNull
import timber.log.Timber

private const val TAG = "PubNubChannelService"

object PubNubChannelService : EventChannel {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        Timber.tag("Coroutine Exception").d("Handled...")
        e.printStackTrace()
    }

    var isReconnecting = false

    private val mutex = Mutex(false)

    private var pubnub: PubNub? = null
    //private val channelName = Utils.uuid

    private val eventFlow = MutableSharedFlow<Communication>(replay = 0)

    private val pubNubData by lazy {
        PubNubSubscriber.getSubscribeCallback(ioScope)
    }

    private val ioScope by lazy { CoroutineScope(Dispatchers.IO + coroutineExceptionHandler) }

    private val config by lazy {
        PNConfiguration().apply {
            logVerbosity = PNLogVerbosity.BODY
        }
    }

    override fun reconnect() {
        Log.d(TAG, "reconnect: Pubnub")
        if (isReconnecting.not()) {
            Log.d(TAG, "reconnect: Pubnub .....")
            ioScope.launch {
                mutex.withLock {
                    isReconnecting = true
                    pubnub?.removeListener(pubNubData.callback)
                    pubnub?.unsubscribeAll()
                    pubnub?.reconnect()
                    pubnub?.addListener(pubNubData.callback)
                    pubnub?.subscribe()
                        ?.channels(listOf(Utils.uuid))
                        ?.execute()
                    isReconnecting = false
                }
            }
        }
    }

    override suspend fun initChannel() {
        voipLog?.log("Start PubNub Init")
        withContext(ioScope.coroutineContext) {
            voipLog?.log("Coroutine Started for PubNub")
            if (pubnub == null)
                synchronized(this) {
                    if (pubnub != null)
                        pubnub
                    else {
                        config.publishKey = BuildConfig.PUBNUB_PUB_P2P_KEY
                        config.subscribeKey = BuildConfig.PUBNUB_SUB_P2P_KEY
                        config.reconnectionPolicy = PNReconnectionPolicy.LINEAR
                        config.httpLoggingInterceptor =
                            HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                        //config.uuid = "Mentor.getInstance().getId()"
                        pubnub = PubNub(config)
                        pubnub?.addListener(pubNubData.callback)
                        pubnub?.subscribe()
                            ?.channels(listOf(Utils.uuid))
                            ?.execute()
                        observeIncomingMessage()
                    }
                }
            voipLog?.log("Coroutine Ended for PubNub --> $pubnub")
        }
    }

    override fun emitEvent(event: OutgoingData) {
        ioScope.launch {
            try {
                val message = when (event) {
                    is NetworkActionData -> event as NetworkAction
                    is UserActionData -> event as UserAction
                    is Timeout -> event
                }

                pubnub?.publish()
                    ?.channel(Utils.uuid)
                    ?.message(message)
                    ?.ttl(0)
                    ?.usePOST(true)
                    ?.sync()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun observeChannelEvents(): SharedFlow<Communication> {
        voipLog?.log("observeChannelEvents: $pubnub")
        return eventFlow
    }

    private fun observeIncomingMessage() {
        ioScope.launch {
            pubNubData.event.collect {
                //if (state == State.ACTIVE)
                Log.d(TAG, "observeIncomingMessage: $it")
                when (it) {
                    is MessageData -> eventFlow.emit(it)
                    is ChannelData -> eventFlow.emit(it)
                    is IncomingCall -> eventFlow.emit(it)
                    is Error -> eventFlow.emit(it)
                }
            }
        }
    }
}