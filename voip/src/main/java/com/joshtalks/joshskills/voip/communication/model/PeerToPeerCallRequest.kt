package com.joshtalks.joshskills.voip.communication.model

import com.joshtalks.joshskills.voip.webrtc.CallRequest

class PeerToPeerCallRequest : CallRequest {
    override fun getChannel(): String {
        return ""
    }

    override fun getToken(): String {
        return ""
    }
}