package com.joshtalks.joshskills.voip.communication

import android.util.Log
import com.google.gson.Gson
import com.joshtalks.joshskills.voip.BuildConfig
import com.joshtalks.joshskills.voip.communication.constants.CHANNEL
import com.joshtalks.joshskills.voip.communication.model.Channel
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.Communication
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.Message
import com.joshtalks.joshskills.voip.communication.model.MessageData
import com.joshtalks.joshskills.voip.communication.model.NetworkAction
import com.joshtalks.joshskills.voip.communication.model.NetworkActionData
import com.joshtalks.joshskills.voip.communication.model.OutgoingData
import com.joshtalks.joshskills.voip.communication.model.UserAction
import com.joshtalks.joshskills.voip.communication.model.UserActionData
import com.joshtalks.joshskills.voip.voipLog
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
private const val TAG = "PubNubChannelService"

object PubNubChannelService : EventChannel {
    enum class State {
        ACTIVE,
        INACTIVE
    }

    private var state = State.INACTIVE
    private var pubnub: PubNub? = null
    private val channelName = "p2p-new-architecture-testing"

    private val eventFlow = MutableSharedFlow<Communication>(replay = 0)

    private val pubNubData by lazy {
        PubNubSubscriber.getSubscribeCallback(scope)
    }

    private val scope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    private val config by lazy {
        PNConfiguration().apply {
            logVerbosity = PNLogVerbosity.BODY
        }
    }

    override suspend fun initChannel() {
        voipLog?.log("Start PubNub Init")
        withContext(scope.coroutineContext) {
            voipLog?.log("Coroutine Started for PubNub")
            if (pubnub == null)
                synchronized(this) {
                    if (pubnub != null)
                        pubnub
                    else {
                        config.publishKey = BuildConfig.PUBNUB_PUB_P2P_KEY
                        config.subscribeKey = BuildConfig.PUBNUB_SUB_P2P_KEY
                        //config.uuid = "Mentor.getInstance().getId()"
                        pubnub = PubNub(config)
                        pubnub?.addListener(pubNubData.callback)
                        pubnub?.subscribe()
                            ?.channels(listOf(channelName))
                            ?.execute()
                        observeIncomingMessage()
                    }
                }
            voipLog?.log("Coroutine Ended for PubNub --> $pubnub")
        }
    }

    override fun emitEvent(event: OutgoingData) {
        scope.launch {
            try {
                val message = when (event) {
                    is NetworkActionData -> event as NetworkAction
                    is UserActionData -> event as UserAction
                }

                pubnub?.publish()
                    ?.channel(channelName)
                    ?.message(message)
                    ?.ttl(0)
                    ?.usePOST(true)
                    ?.sync()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun observeChannelEvents(): Flow<Communication> {
        voipLog?.log("observeChannelEvents: $pubnub")
        return eventFlow
    }

    private fun observeIncomingMessage() {
        scope.launch {
            pubNubData.event.collect {
                //if (state == State.ACTIVE)
                Log.d(TAG, "observeIncomingMessage: $it")
                    when (it) {
                        is MessageData -> eventFlow.emit(it)
                        is ChannelData -> eventFlow.emit(it)
                        is Error -> eventFlow.emit(it)
                    }
            }
        }
    }
}

private class PubNubSubscriber : SubscribeCallback() {

    companion object {
        @Volatile private lateinit var INSTANCE: PubNubSubscriber
        @Volatile private lateinit var scope : CoroutineScope

        private val messageFlow by lazy<MutableSharedFlow<Communication>> {
            MutableSharedFlow(replay = 0)
        }

        fun getSubscribeCallback(scope: CoroutineScope) : PubNubData {
            if (this::INSTANCE.isInitialized)
                return PubNubData(INSTANCE, messageFlow)
            else
                synchronized(this) {
                    return if (this::INSTANCE.isInitialized)
                        PubNubData(INSTANCE, messageFlow)
                    else {
                        Companion.scope = scope
                        PubNubData(PubNubSubscriber().also { INSTANCE = it }, messageFlow)
                    }
                }
        }
    }

    override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

    override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
        scope.launch {
            voipLog?.log("message: $pnMessageResult")
            val messageJson = pnMessageResult.message
            try {
                val message = if (pnMessageResult.userMetadata.asInt == CHANNEL)
                    Gson().fromJson(messageJson, Channel::class.java)
                else
                    Gson().fromJson(messageJson, Message::class.java)
                messageFlow.emit(message)
            } catch (e : Exception) {
                e.printStackTrace()
                messageFlow.emit(Error())
            }

        }
    }

    override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

    override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

    override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

    override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {}

    override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

    override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

    override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
}

private data class PubNubData(val callback: SubscribeCallback, val event : Flow<Communication>)