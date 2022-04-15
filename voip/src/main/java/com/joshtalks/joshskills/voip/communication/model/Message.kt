package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.IncorrectCommunicationDataException
import com.joshtalks.joshskills.voip.communication.model.MessageData

data class Message(

	@field:SerializedName("channel_name")
	private val channelName: String? = null,

	@field:SerializedName("type")
	private val type: Int? = null,

	@field:SerializedName("timetoken")
	private val timeToken: Long? = null

) : MessageData {

	companion object {
		fun fromMap(map: Map<String, Any?>?) : Message {
			return Message(
				channelName = map?.get("channel_name")?.toString(),
				type = map?.get("type")?.toString()?.toInt(),
				timeToken = map?.get("timetoken")?.toString()?.toLong(),
			)
		}
	}

	override fun getType(): Int {
		return type ?: throw IncorrectCommunicationDataException("Call Type is NULL")
	}

	override fun getEventTime(): Long? {
		return timeToken
	}

	override fun getChannel(): String {
		return channelName ?: throw IncorrectCommunicationDataException("Channel is NULL")
	}
}
