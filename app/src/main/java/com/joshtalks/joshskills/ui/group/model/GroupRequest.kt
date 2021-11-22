package com.joshtalks.joshskills.ui.group.model

import com.google.gson.annotations.SerializedName

open class GroupRequest(
	@field:SerializedName("mentor_id")
	val mentorId: String,

	@field:SerializedName("group_id")
	val groupId: String,
)

class TimeTokenRequest(mentorId: String, groupId: String, @field:SerializedName("time_token")
val timeToken: Long,) : GroupRequest(mentorId, groupId)
