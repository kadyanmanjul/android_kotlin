package com.joshtalks.joshskills.premium.calling.communication.model

import com.google.gson.annotations.SerializedName

class GroupIncomingCall(
    @field:SerializedName("call_id")
    private val callId: Int? = null,

    @field:SerializedName("type")
    private val type: Int? = null,

    @field:SerializedName("timetoken")
    private val timeToken: Long? = null,

    @field:SerializedName("group_name")
    private val groupName: String? = null,

    @field:SerializedName("group_img")
    private val groupImage: String? = null

) : IncomingCallData {

    companion object {
        fun fromMap(map: Map<String, Any?>?): GroupIncomingCall {
            return GroupIncomingCall(
                callId = map?.get("call_id").toString().toInt(),
                type = map?.get("type").toString().toInt(),
                timeToken = map?.get("timetoken").toString().toLong(),
                groupName = map?.get("group_name").toString(),
                groupImage = map?.get("group_img").toString(),
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

    fun getGroupImage() : String {
        return groupImage ?: ""
    }

    fun getGroupName() : String {
        return groupName ?: throw IncorrectCommunicationDataException("Incoming Call GROUP_NAME is NULL")
    }
}