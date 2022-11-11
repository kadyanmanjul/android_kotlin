package com.joshtalks.joshskills.ui.voip.new_arch.ui.models

import com.google.gson.annotations.SerializedName

data class AutoConnectData(

	@field:SerializedName("sub_header")
	val subHeader: String? = null,

	@field:SerializedName("condition")
	val condition: String? = null,

	@field:SerializedName("wait_time")
	val waitTime: Long? = null,

	@field:SerializedName("header")
	val header: String? = null
)
