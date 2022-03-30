package com.joshtalks.joshskills.voip.data.api

import com.google.gson.annotations.SerializedName

data class ConnectionRequest(

	@field:SerializedName("course_id")
	val courseId: Int? = null,

	@field:SerializedName("mentor_id")
	val mentorId: String? = null,

	@field:SerializedName("topic_id")
	val topicId: Int? = null
)
