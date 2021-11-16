package com.joshtalks.joshskills.ui.group.model

interface GroupMember {
    fun getMentorID(): String
    fun getMemberName(): String
    fun memberIcon(): String
    fun isAdmin(): Boolean
    fun isOnline(): Boolean
}