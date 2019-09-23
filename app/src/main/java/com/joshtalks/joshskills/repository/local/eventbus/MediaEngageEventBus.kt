package com.joshtalks.joshskills.repository.local.eventbus

import com.joshtalks.joshskills.repository.local.model.ListenGraph

data class MediaEngageEventBus(var type: String, var id: String, var list: List<ListenGraph>)
