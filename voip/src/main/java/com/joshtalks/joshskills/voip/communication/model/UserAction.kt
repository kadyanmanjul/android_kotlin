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

// TODO: Different Class
data class UI(

	@field:SerializedName("type")
	private val type: Int,

	@field:SerializedName("channel_name")
	private val channelName: String,

	@field:SerializedName("is_hold")
	private val isHold: Int,

	@field:SerializedName("is_mute")
	private val isMute: Int,

	@field:SerializedName("is_play_btn_click")
	private val isPlayButtonClick: Int,

	private val address : String

) : UIState {

	companion object {
		fun fromMap(map: Map<String, Any?>?) : UI {
			return UI(
				channelName = map?.get("channelName").toString(),
				type =  map?.get("type").toString().toInt(),
				isHold = map?.get("is_hold").toString().toInt(),
				isMute = map?.get("is_mute").toString().toInt(),
				isPlayButtonClick = map?.get("is_play_btn_click").toString().toInt(),
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

	override fun isPlayBtnClick(): Boolean {
		return isPlayButtonClick == 1
	}

	override fun getChannelName(): String {
		return channelName
	}

	override fun getAddress(): String {
		return address
	}
}
