package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.ui.userprofile.models.Award

data class AwardItemClickedEventBus(val award: Award)