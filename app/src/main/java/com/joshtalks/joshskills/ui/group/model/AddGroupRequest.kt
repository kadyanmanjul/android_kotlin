package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

data class AddGroupRequest(

	@field:SerializedName("mentor_id")
	val mentorId: String,

	@field:SerializedName("group_name")
	val groupName: String,

	@field:SerializedName("group_icon")
	var groupIcon: String
)
