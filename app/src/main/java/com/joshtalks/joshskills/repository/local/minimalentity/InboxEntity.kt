package com.joshtalks.joshskills.repository.local.minimalentity

import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.User
import java.io.Serializable

data class InboxEntity constructor(
    val course_name: String = "",
    val conversation_id: String = "",
    val course_icon: String? = "",
    val courseId: String? = "",
    val duration: Int? = 0,
    val is_deleted: Boolean? = false,
    val teacher_id: String? = "",
    var chat_id: String? = "",
    var created: Long? = 0,
    var courseCreatedDate: Long? = 0,
    var isSeen: Boolean? = false,
    var text: String? = "",
    var type: BASE_MESSAGE_TYPE? = BASE_MESSAGE_TYPE.TX,
    var url: String? = "",
    var localUrlPath: String? = "",
    var title: String? = "",
    var question_type: String? = "",
    var qText: String? = "",
    var user: User? = null,
    var material_type: BASE_MESSAGE_TYPE?,
    var message_deliver_status: MESSAGE_DELIVER_STATUS? = MESSAGE_DELIVER_STATUS.READ

):Serializable