package com.joshtalks.joshskills.ui.voip.user_presence

sealed class PresenceStatus(){
    object Online: PresenceStatus()
    object Offline: PresenceStatus()
}