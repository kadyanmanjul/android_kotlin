package com.joshtalks.badebhaiya.recordedRoomPlayer

import kotlinx.coroutines.flow.MutableSharedFlow

object PlayerData {

    val isLoading = MutableSharedFlow<Boolean>()

}