package com.joshtalks.joshskills.premium.calling.communication.model

import com.google.gson.annotations.SerializedName

class  FppIncomingCall(
    @field:SerializedName("call_id")
    private val callId: Int? = null,

    @field:SerializedName("type")
    private val type: Int? = null,

    @field:SerializedName("timetoken")
    private val timeToken: Long? = null,

    @field:SerializedName("fpp_name")
    private val fppName: String? = null,

    @field:SerializedName("fpp_img")
    private val fppImage: String? = null

) : IncomingCallData {

    companion object {
        fun fromMap(map: Map<String, Any?>?): FppIncomingCall {
            return FppIncomingCall(
                callId = map?.get("call_id").toString().toInt(),
                type = map?.get("type").toString().toInt(),
                timeToken = map?.get("timetoken").toString().toLong(),
                fppName = map?.get("fpp_name").toString(),
                fppImage = map?.get("fpp_img").toString(),
            )
        }
    }

    override fun getCallId(): Int {
        return callId ?: throw IncorrectCommunicationDataException("Incoming Call CALL_ID is NULL")
    }

    override fun getType(): Int {
        return type ?: throw IncorrectCommunicationDataException("Incoming Call_TYPE is NULL")
    }

    override fun getEventTime(): Long? {
        return timeToken
    }

    fun getFppImage() : String {
        return fppImage ?: ""
    }

    fun getFppName() : String {
        return fppName ?: throw IncorrectCommunicationDataException("Incoming Call GROUP_NAME is NULL")
    }
}