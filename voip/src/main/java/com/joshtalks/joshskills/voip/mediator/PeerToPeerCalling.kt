package com.joshtalks.joshskills.voip.mediator

import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.voip.Utils
import com.joshtalks.joshskills.voip.data.api.ConnectionRequest
import com.joshtalks.joshskills.voip.data.api.VoipNetwork
import com.joshtalks.joshskills.voip.voipLog

class PeerToPeerCalling : Calling {
    val voipNetwork = VoipNetwork.getVoipApi()

    override suspend fun onPreCallConnect(callData: HashMap<String, Any>) {
        voipLog?.log("Calling API ---- $callData")
        val request = ConnectionRequest(
            topicId = (callData[INTENT_DATA_TOPIC_ID] as String).toInt(),
            mentorId = Utils.uuid,
            courseId = (callData[INTENT_DATA_COURSE_ID] as String).toInt()
        )
        voipLog?.log("Calling API ---- $request")
        val response = voipNetwork.setUpConnection(request)
        if(response.isSuccessful)
            voipLog?.log("Sucessfull")
    }

    override fun onCallDisconnect() {
        TODO("Not yet implemented")
    }
}