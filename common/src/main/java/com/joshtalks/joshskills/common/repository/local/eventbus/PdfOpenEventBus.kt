package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.entity.PdfType

data class PdfOpenEventBus(var chatId: String, var pdfObject: PdfType)