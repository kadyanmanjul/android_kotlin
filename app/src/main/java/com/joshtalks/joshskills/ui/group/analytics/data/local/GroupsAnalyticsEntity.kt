package com.joshtalks.joshskills.ui.group.analytics.data.local

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