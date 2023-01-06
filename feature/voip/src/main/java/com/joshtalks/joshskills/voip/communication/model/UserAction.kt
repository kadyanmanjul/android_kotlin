package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName

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

data class UI(

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("channel_name")
	private val channelName: String,

	@field:SerializedName("is_hold")
	private val isHold: Int,

	@field:SerializedName("is_mute")
	private val isMute: Int,

	private val address : String

) : UIState {

	companion object {
		fun fromMap(map: Map<String, Any?>?) : UI {
			return UI(
				channelName = map?.get("channelName").toString(),
				type =  map?.get("type").toString().toInt(),
				isHold = map?.get("is_hold").toString().toInt(),
				isMute = map?.get("is_mute").toString().toInt(),
				address = ""
			)
		}
	}

	override fun getType(): Int {
		return type
	}

	override fun isHold(): Boolean {
		return isHold == 1
	}

	override fun isMute(): Boolean {
		return isMute == 1
	}

	override fun getChannelName(): String {
		return channelName
	}

	override fun getAddress(): String {
		return address
	}
}

data class Interest(

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("channel_name")
	private val channelName: String,

	@field:SerializedName("interest_header")
	private val header: String,

	@field:SerializedName("common_interests")
	private val interests: List<String>,

	@field:SerializedName("timetoken")
	private val timeToken: Long? = null,

	) : InterestData {

	override fun getInterestHeader(): String {
		return header
	}

	override fun getInterests(): List<String> {
		return interests
	}

	override fun getChannel(): String {
		return channelName
	}

	override fun getType(): Int {
		return type
	}

	override fun getEventTime(): Long? {
		return timeToken
	}
}