package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.NetworkActionData

data class NetworkAction(

	@field:SerializedName("duration")
	private val duration: Long,

	@field:SerializedName("uid")
	private val uid: Int,

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("channel_name")
	private val channelName: String,

	private val address : String

) : NetworkActionData {
	override fun getType(): Int {
		return type
	}

	override fun getChannelName(): String {
		return channelName
	}

	override fun getUserId(): Int {
		return uid
	}

	override fun getDuration(): Long {
		return duration
	}

	override fun getAddress(): String {
		return address
	}
}
