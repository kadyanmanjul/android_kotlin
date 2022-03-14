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

	@field:SerializedName("call_id")
	private val callId: Int
) : NetworkActionData {
	override fun getType(): Int {
		return type
	}

	override fun getCallingId(): Int {
		return callId
	}

	override fun getUserId(): Int {
		return uid
	}

	override fun getDuration(): Long {
		return duration
	}
}
