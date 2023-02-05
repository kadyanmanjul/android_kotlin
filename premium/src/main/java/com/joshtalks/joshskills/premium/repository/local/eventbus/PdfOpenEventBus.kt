package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.local.entity.PdfType

data class PdfOpenEventBus(var chatId: String, var pdfObject: PdfType)