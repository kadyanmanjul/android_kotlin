package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.voip.communication.EventChannel
import com.joshtalks.joshskills.voip.communication.PubNubChannelService
import com.joshtalks.joshskills.voip.communication.constants.DISCONNECTED
import com.joshtalks.joshskills.voip.communication.constants.ONHOLD
import com.joshtalks.joshskills.voip.communication.model.ChannelData
import com.joshtalks.joshskills.voip.communication.model.Error
import com.joshtalks.joshskills.voip.communication.model.IncomingCall
import com.joshtalks.joshskills.voip.communication.model.MessageData
import com.joshtalks.joshskills.voip.communication.model.PeerToPeerCallRequest
import com.joshtalks.joshskills.voip.constant.CALL_CONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_DISCONNECTED
import com.joshtalks.joshskills.voip.constant.CALL_INITIATED
import com.joshtalks.joshskills.voip.constant.HOLD
import com.joshtalks.joshskills.voip.constant.RECONNECTED
import com.joshtalks.joshskills.voip.constant.RECONNECTING
import com.joshtalks.joshskills.voip.voipLog
import com.joshtalks.joshskills.voip.webrtc.AgoraCallingService
import com.joshtalks.joshskills.voip.webrtc.CallState
import com.joshtalks.joshskills.voip.webrtc.CallingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PeerToPeerCalling : Calling {
    override fun onCallDisconnect() {
        TODO("Not yet implemented")
    }
}