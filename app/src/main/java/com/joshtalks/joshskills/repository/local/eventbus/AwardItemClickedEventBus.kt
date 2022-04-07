package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.ui.userprofile.models.Award

data class AwardItemClickedEventBus(val award: Award)