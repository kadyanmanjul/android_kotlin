package com.joshtalks.joshskills.ui.group.model

data class GroupMember (
    val getMentorID: String,
    val getMemberName: String,
    val memberIcon: String,
    val isAdmin: Boolean,
    var isOnline: Boolean
)