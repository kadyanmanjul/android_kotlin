package com.joshtalks.joshskills.repository.local.minimalentity

import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import java.io.Serializable
import java.util.*


data class CourseContentEntity constructor(
    val conversation_id: String = "",
    var chat_id: String = "",
    var created: Date,
    var title: String? = "",
    var material_type: BASE_MESSAGE_TYPE?,
    var type: BASE_MESSAGE_TYPE?,
    var question_type: BASE_MESSAGE_TYPE?,
    var questionId: String? = ""


) : Serializable