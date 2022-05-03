package com.joshtalks.joshskills.voip.state

interface VoipState {
    fun connect() {}
    fun disconnect() {}
    fun backPress() {}
    fun onError()
    fun onDestroy()
}