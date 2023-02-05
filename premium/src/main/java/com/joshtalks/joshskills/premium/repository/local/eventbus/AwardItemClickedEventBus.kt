package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.ui.userprofile.models.Award

data class AwardItemClickedEventBus(val award: Award)