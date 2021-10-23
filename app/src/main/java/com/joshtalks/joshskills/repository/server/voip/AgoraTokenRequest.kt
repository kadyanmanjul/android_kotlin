package com.joshtalks.joshskills.repository.server.voip

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.Mentor

data class AgoraTokenRequest(
    @SerializedName("mentor_id")
    var mentorId: String = Mentor.getInstance().getId(),
    @SerializedName("course_id")
    var courseId: String?,
    @SerializedName("is_demo")
    var isDemo: Boolean,
    @SerializedName("topic_id")
    var topicId: String?,
    @SerializedName("group_id")
    var groupId: String? = null
)