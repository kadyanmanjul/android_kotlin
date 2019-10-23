package com.joshtalks.joshskills.repository.local.eventbus

data class MediaProgressEventBus( val state: Int,val id: String, val progress:Float)