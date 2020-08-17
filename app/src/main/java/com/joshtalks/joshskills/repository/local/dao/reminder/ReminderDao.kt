package com.joshtalks.joshskills.repository.local.dao.reminder

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReminder(reminderItem: ReminderResponse)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllReminders(reminderList: List<ReminderResponse>)

    @Query("SELECT * FROM REMINDER_TABLE WHERE status <> 'DELETED' ORDER BY reminder_time ASC")
    fun getAllReminders(): LiveData<List<ReminderResponse>>

    @Update
    fun updateReminder(reminderItem: ReminderResponse)
}