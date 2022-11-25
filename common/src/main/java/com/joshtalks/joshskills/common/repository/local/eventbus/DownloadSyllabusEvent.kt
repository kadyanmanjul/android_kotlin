package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.server.course_detail.SyllabusData

data class DownloadSyllabusEvent(val syllabusData: SyllabusData)