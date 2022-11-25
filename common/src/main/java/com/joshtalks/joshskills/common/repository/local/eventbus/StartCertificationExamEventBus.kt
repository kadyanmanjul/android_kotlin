package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.CExamStatus

class StartCertificationExamEventBus(
    var conversationId: String,
    var messageId: String,
    var certificationExamId: Int,
    var examStatus: CExamStatus,
    var lessonInterval: Int?
)