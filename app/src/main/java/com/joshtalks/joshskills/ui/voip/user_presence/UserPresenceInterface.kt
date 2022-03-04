package com.joshtalks.joshskills.ui.voip.user_presence

interface UserPresenceInterface {
    fun setUserPresence(key: String, value: PresenceStatus)
}