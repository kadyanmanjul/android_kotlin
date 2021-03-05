package com.joshtalks.joshskills.repository.local.eventbus

data class OpenUserProfile(
    val id: String?,
    val isUserOnline:Boolean=false
)