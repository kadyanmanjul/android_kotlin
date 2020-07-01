package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.course_detail.SyllabusData

data class DownloadSyllabusEvent(val syllabusData: SyllabusData)