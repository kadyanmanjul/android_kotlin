package com.joshtalks.joshskills.premium.calling.state

interface VoipState {
    fun connect() {}
    fun disconnect() {}
    fun backPress() {}
    fun onError(reason: String)
    fun onDestroy()
}