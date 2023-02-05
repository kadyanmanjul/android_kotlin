package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.CExamStatus

class StartCertificationExamEventBus(
    var conversationId: String,
    var messageId: String,
    var certificationExamId: Int,
    var examStatus: CExamStatus,
    var lessonInterval: Int?
)