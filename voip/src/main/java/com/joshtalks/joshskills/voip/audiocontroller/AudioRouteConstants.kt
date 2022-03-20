package com.joshtalks.joshskills.voip.audiocontroller

const val HEADSET_CONNECTED = 1
const val HEADSET_DISCONNECTED = 0

sealed class AudioRouteConstants {
    object BluetoothAudio : AudioRouteConstants()
    object HeadsetAudio : AudioRouteConstants()
    object SpeakerAudio : AudioRouteConstants()
    object EarpieceAudio : AudioRouteConstants()
}