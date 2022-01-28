package com.joshtalks.joshskills.quizgame.analytics.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_analytics")
data class GameAnalyticsEntity(
    val event: String,
    val mentorId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}