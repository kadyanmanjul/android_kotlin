package com.joshtalks.joshskills.common.repository.local.eventbus

data class OpenUserProfile(
    val id: String?,
    val isUserOnline:Boolean=false
)