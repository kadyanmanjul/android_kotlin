package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.UserActionData

data class UserAction(

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("channel_name")
	private val channelName: String,

	private val address : String
) : UserActionData {
	override fun getType(): Int {
		return type
	}

	override fun getChannelName(): String {
		return channelName
	}

	override fun getAddress(): String {
		return address
	}
}
