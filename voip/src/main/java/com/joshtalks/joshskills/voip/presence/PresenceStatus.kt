package com.joshtalks.joshskills.voip.presence

sealed class PresenceStatus {
    object Online: PresenceStatus()
    object Offline: PresenceStatus()
}