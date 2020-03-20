package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.server.engage.Graph

data class MediaEngageEventBus(var type: String, var id: String, var list: List<Graph>)
