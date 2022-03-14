package com.joshtalks.joshskills.ui.voip.new_arch.user_presence

sealed class PresenceStatus {
    object Online: PresenceStatus()
    object Offline: PresenceStatus()
}