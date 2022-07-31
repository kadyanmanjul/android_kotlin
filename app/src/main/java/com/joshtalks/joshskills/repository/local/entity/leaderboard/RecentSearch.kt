package com.joshtalks.joshskills.repository.local.entity.leaderboard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecentSearch(
    @PrimaryKey
    @ColumnInfo(name = "keyword")
    var keyword: String,

    @ColumnInfo(name = "timestamp")
    var timestamp: Long
)