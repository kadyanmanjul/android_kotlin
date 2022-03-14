package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.communication.model.UserActionData

data class UserAction(

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("call_id")
	private val callId: Int
) : UserActionData {
	override fun getType(): Int {
		return type
	}

	override fun getCallingId(): Int {
		return callId
	}
}
