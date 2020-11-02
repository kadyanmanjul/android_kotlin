package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.day_wise_course.reading.PraticeAudioViewHolder

data class RemovePracticeAudioEventBus(
    val practiceEngagementId: Int?,
    val praticeAudioViewHolder: PraticeAudioViewHolder
)
