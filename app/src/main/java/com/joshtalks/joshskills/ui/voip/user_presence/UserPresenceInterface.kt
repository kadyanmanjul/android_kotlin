package com.joshtalks.joshskills.ui.voip.user_presence

interface UserPresenceInterface {
    fun setUserPresenceInDB(key: String, value: PresenceStatus)
}