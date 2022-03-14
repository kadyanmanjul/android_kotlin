package com.joshtalks.joshskills.ui.voip.new_arch.audio_controller

const val HEADSET_CONNECTED = 1
const val HEADSET_DISCONNECTED = 0

sealed class AudioRouteConstants {
    object BluetoothAudio : AudioRouteConstants()
    object HeadsetAudio : AudioRouteConstants()
    object NormalAudio : AudioRouteConstants()
}