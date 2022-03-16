package com.joshtalks.joshskills.voip.communication.model

import com.joshtalks.joshskills.voip.webrtc.CallRequest

data class PeerToPeerCallRequest(private val channelName: String, private val callToken :  String) : CallRequest {
    override fun getChannel(): String {
        return channelName
    }

    override fun getToken(): String {
        return callToken
    }
}