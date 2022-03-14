package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.IncomingCallData
import com.joshtalks.joshskills.voip.communication.model.IncorrectCommunicationDataException

class IncomingCall (
    @field:SerializedName("call_id")
    private val callId: Int? = null,

    @field:SerializedName("type")
    private val type: Int? = null
) : IncomingCallData {
    override fun getCallId(): Int {
        return callId ?: throw IncorrectCommunicationDataException("Incoming Call CALL ID is NULL")
    }

    override fun getType(): Int {
        return type ?: throw IncorrectCommunicationDataException("Incoming Call TYPE is NULL")
    }
}