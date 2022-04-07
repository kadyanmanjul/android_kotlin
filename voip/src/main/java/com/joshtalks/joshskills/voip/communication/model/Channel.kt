package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName

data class Channel(

	@field:SerializedName("channel_name")
	private val channelName: String? = null,

	@field:SerializedName("partner_name")
	private val partnerName: String? = null,

	@field:SerializedName("topic_name")
	private val topicName: String? = null,

	@field:SerializedName("partner_image")
	private val partnerImage: String? = null,

	@field:SerializedName("type")
	private val type: Int? = null,

	@field:SerializedName("token")
	private val token: String? = null,

	@field:SerializedName("call_id")
	private val callId: Int? = null,

	@field:SerializedName("agora_uid")
	private val agoraUId: Int? = null,

	@field:SerializedName("partner_uid")
	private val partnerUid: Int?
) : ChannelData {
	override fun getCallingPartnerName(): String {
		return partnerName ?: "Unable to fetch Name"
	}

	override fun getCallingPartnerImage(): String? {
		return partnerImage
	}

	override fun getCallingTopic(): String {
		return topicName ?: "Unable to fetch Topic"
	}

	override fun getCallingId(): Int {
		return callId ?: throw IncorrectCommunicationDataException("Call ID is NULL")
	}

	override fun getCallingToken(): String {
		return token ?: throw IncorrectCommunicationDataException("Call Token is NULL")
	}

	override fun getAgoraUid(): Int {
		return agoraUId ?: throw IncorrectCommunicationDataException("Agora UID is NULL")
	}

	override fun getPartnerUid(): Int {
		return partnerUid ?: throw IncorrectCommunicationDataException("Partner UID is NULL")
	}

	override fun getType(): Int {
		return type ?: throw IncorrectCommunicationDataException("Call type is NULL")
	}

	override fun getChannel(): String {
		return channelName ?: throw IncorrectCommunicationDataException("Channel is NULL")
	}
}