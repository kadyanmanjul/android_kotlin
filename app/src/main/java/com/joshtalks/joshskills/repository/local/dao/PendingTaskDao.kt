package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel

@Dao
interface PendingTaskDao {
    @Insert
    suspend fun insertPendingTask(pendingTask: PendingTaskModel): Long

    @Query("SELECT * FROM pending_task_table")
    suspend fun getPendingTasks(): List<PendingTaskModel>

    @Query("DELETE FROM pending_task_table WHERE id =:id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM pending_task_table WHERE id=:id")
    suspend fun getTask(id: Long): PendingTaskModel?

    @Query("UPDATE pending_task_table SET retry_count = :numberOfRetries WHERE id = :id")
    fun updateRetryCount(id: Long, numberOfRetries: Int)
}