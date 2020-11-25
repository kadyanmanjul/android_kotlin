package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.Award

data class AwardItemClickedEventBus(val award: Award)