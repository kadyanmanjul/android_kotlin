package com.joshtalks.joshskills.common.repository.local.eventbus

import com.joshtalks.joshskills.common.repository.server.engage.Graph

data class MediaEngageEventBus(var type: String, var id: String, var list: List<Graph>)
