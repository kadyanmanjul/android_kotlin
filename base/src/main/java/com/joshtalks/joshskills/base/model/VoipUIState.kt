package com.joshtalks.joshskills.base.model

data class VoipUIState(
    val isMute: Boolean,
    val isSpeakerOn: Boolean,
    val isOnHold: Boolean,
    val isRemoteUserMuted: Boolean
    )