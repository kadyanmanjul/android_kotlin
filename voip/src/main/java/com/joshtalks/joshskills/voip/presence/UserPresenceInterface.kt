package com.joshtalks.joshskills.voip.presence

internal interface UserPresenceInterface {
    fun setUserPresence(key: String, value: PresenceStatus)
}