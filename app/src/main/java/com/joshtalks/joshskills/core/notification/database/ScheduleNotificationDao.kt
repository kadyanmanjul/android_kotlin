package com.joshtalks.joshskills.core.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.core.notification.model.ScheduleNotification

@Dao
interface ScheduleNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notification: List<ScheduleNotification>)

    @Query("SELECT * FROM schedule_notification WHERE category = :category AND is_scheduled = 0")
    suspend fun getCategoryNotifications(category: String): List<ScheduleNotification>

    @Query("UPDATE schedule_notification SET is_scheduled = 1 WHERE id = :id")
    suspend fun updateScheduled(id: String)

    @Query("UPDATE schedule_notification SET is_shown = 1 WHERE id = :id")
    suspend fun updateShown(id: String)

    @Query("SELECT * FROM schedule_notification WHERE id = :notificationId")
    fun getNotification(notificationId: String): ScheduleNotification

}