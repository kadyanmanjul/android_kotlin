package com.joshtalks.joshskills.premium.repository.local.eventbus

import com.joshtalks.joshskills.premium.repository.server.engage.Graph

data class MediaEngageEventBus(var type: String, var id: String, var list: List<Graph>)
