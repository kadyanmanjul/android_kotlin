package com.joshtalks.joshskills.ui.group.db.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_list_table")
data class GroupListEntity(
    @PrimaryKey
    val groupId: String,
    val lastMessage : String? = null,
    val lastMsgTime: String? = null,
    val unreadCount: String? = null,
)