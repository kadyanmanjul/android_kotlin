package com.joshtalks.joshskills.ui.voip.new_arch.user_presence

interface UserPresenceInterface {
    fun setUserPresence(key: String, value: PresenceStatus)
}