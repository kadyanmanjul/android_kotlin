package com.joshtalks.badebhaiya.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object NetworkManager {
    val networkSlowFlow = MutableSharedFlow<Boolean>()

    fun networkIsFlow(){
        CoroutineScope(Dispatchers.IO).launch {
            delay(200)
            networkSlowFlow.emit(true)
        }
    }
}