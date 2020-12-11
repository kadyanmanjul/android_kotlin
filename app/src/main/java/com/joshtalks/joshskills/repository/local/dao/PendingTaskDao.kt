package com.joshtalks.joshskills.repository.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel

@Dao
interface PendingTaskDao {
    @Insert
    fun insertPendingTask(pendingTask: PendingTaskModel): Int

    @Query("SELECT * FROM pending_task_table")
    fun getPendingTasks()

    @Query("DELETE FROM pending_task_table WHERE id =:id")
    fun deleteTask(id: Int)
}