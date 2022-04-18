package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.IncomingCallData
import com.joshtalks.joshskills.voip.communication.model.IncorrectCommunicationDataException

class IncomingCall (
    @field:SerializedName("call_id")
    private val callId: Int? = null,

    @field:SerializedName("type")
    private val type: Int? = null,

    @field:SerializedName("timetoken")
    private val timeToken: Long? = null

) : IncomingCallData {

    companion object {
        fun fromMap(map: Map<String, Any?>?) : IncomingCall {
            return IncomingCall(
                callId = map?.get("call_id").toString().toInt(),
                type =  map?.get("type").toString().toInt(),
                timeToken = map?.get("timetoken").toString().toLong(),
            )
        }
    }

    override fun getCallId(): Int {
        return callId ?: throw IncorrectCommunicationDataException("Incoming Call CALL ID is NULL")
    }

    override fun getType(): Int {
        return type ?: throw IncorrectCommunicationDataException("Incoming Call TYPE is NULL")
    }

    override fun getEventTime(): Long? {
        return timeToken
    }
}