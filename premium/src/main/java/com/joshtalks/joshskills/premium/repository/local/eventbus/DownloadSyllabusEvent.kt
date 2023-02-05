package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.server.course_detail.SyllabusData

data class DownloadSyllabusEvent(val syllabusData: SyllabusData)