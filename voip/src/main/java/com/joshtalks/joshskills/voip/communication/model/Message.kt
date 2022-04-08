package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.IncorrectCommunicationDataException
import com.joshtalks.joshskills.voip.communication.model.MessageData

data class Message(

	@field:SerializedName("channel_name")
	private val channelName: String? = null,

	@field:SerializedName("type")
	private val type: Int? = null
) : MessageData {
	override fun getType(): Int {
		return type ?: throw IncorrectCommunicationDataException("Call Type is NULL")
	}

	override fun getChannel(): String {
		return channelName ?: throw IncorrectCommunicationDataException("Channel is NULL")
	}
}
