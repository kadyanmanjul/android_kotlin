package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.lesson.reading.PracticeAudioViewHolder

data class RemovePracticeAudioEventBus(
    val practiceEngagementId: Int?,
    val practiceAudioViewHolder: PracticeAudioViewHolder
)
