package com.joshtalks.badebhaiya.recordedRoomPlayer

import kotlinx.coroutines.flow.MutableSharedFlow

object PlayerData {

    val isLoading = MutableSharedFlow<Boolean>()

    val endPlayer = MutableSharedFlow<Boolean>()

    val initPlayer = MutableSharedFlow<Boolean>()

}