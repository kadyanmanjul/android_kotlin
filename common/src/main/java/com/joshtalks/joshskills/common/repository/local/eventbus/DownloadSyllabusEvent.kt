package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.local.model.explore.SyllabusData

data class DownloadSyllabusEvent(val syllabusData: SyllabusData)