package com.joshtalks.joshskills.ui.voip.presence

sealed class PresenceStatus {
    object Online: PresenceStatus()
    object Offline: PresenceStatus()
}