package com.joshtalks.joshskills.ui.voip.presence

internal interface UserPresenceInterface {
    fun setUserPresence(key: String, timeStamp: Long?)
}