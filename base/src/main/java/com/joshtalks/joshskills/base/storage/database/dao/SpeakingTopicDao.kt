package com.joshtalks.joshskills.base.storage.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SpeakingTopicDao {

    @Query(value = "SELECT * FROM SpeakingTopic  where id=:topicId")
    suspend fun getTopicById(topicId: String): SpeakingTopic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTopic(topic: SpeakingTopic)

}