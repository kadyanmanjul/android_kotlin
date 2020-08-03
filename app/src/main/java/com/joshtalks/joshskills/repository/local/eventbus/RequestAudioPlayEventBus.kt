package com.joshtalks.joshskills.repository.local.eventbus


data class RequestAudioPlayEventBus(var position: Int, var url: String, var duration: Int)