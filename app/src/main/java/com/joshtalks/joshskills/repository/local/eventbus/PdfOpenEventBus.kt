package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.entity.PdfType

data class PdfOpenEventBus(var chatId: String, var pdfObject: PdfType)