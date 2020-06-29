package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.course_detail.SyllabusData
import com.joshtalks.joshskills.ui.payment.viewholder.SyllabusViewHolder

data class DownloadSyllabusEvent(
    val syllabusData: SyllabusData,
    val syllabusViewHolder: SyllabusViewHolder
)