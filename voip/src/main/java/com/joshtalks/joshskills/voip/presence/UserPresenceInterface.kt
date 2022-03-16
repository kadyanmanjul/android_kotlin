package com.joshtalks.joshskills.voip.presence

interface UserPresenceInterface {
    fun setUserPresence(key: String, value: PresenceStatus)
}