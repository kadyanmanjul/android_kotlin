package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.server.RequestEngage

@Dao
interface PendingTaskDao {
    @Insert
    suspend fun insertPendingTask(pendingTask: PendingTaskModel)

    @Query("SELECT * FROM pending_task_table")
    suspend fun getPendingTasks():List<PendingTaskModel>

    @Query("DELETE FROM pending_task_table WHERE id =:id")
    suspend fun deleteTask(id: Int)
}