package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.CExamStatus

class StartCertificationExamEventBus(
    var conversationId: String,
    var messageId: String,
    var certificationExamId: Int,
    var examStatus: CExamStatus
)