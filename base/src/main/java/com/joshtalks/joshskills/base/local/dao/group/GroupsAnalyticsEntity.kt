package com.joshtalks.joshskills.base.local.dao.group

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups_analytics")
data class GroupsAnalyticsEntity(
    val event: String,
    val mentorId : String,
    val groupId : String?
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}

@Entity(tableName = "group_chat_analytics")
data class GroupChatAnalyticsEntity(
    @PrimaryKey
    val groupId: String,
    val lastSentMsgTime: Long
)