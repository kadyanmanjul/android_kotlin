package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.BroadCastEvent

@Dao
interface BroadCastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBroadcastEvent(event: BroadCastEvent)

    @Query("SELECT * FROM broadcast_events")
    suspend fun getAllEvents(): List<BroadCastEvent>

    @Query("DELETE FROM broadcast_events WHERE id = :id")
    suspend fun deleteEvent(id: Long)
}