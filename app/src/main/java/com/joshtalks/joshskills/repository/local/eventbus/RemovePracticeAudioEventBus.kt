package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.day_wise_course.reading.PracticeAudioViewHolder

data class RemovePracticeAudioEventBus(
    val practiceEngagementId: Int?,
    val practiceAudioViewHolder: PracticeAudioViewHolder
)
