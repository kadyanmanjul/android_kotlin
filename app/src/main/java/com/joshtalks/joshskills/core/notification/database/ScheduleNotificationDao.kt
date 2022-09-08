package com.joshtalks.joshskills.core.notification.database

import androidx.room.*
import com.joshtalks.joshskills.core.notification.model.ScheduleNotification

@Dao
interface ScheduleNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notification: List<ScheduleNotification>)

    @Query("SELECT * FROM schedule_notification WHERE category = :category AND is_scheduled = 0")
    suspend fun getCategoryNotifications(category: String): List<ScheduleNotification>

    @Query("SELECT id FROM schedule_notification WHERE category = :category")
    fun getIdFromCategory(category: String): List<String>

    @Query("UPDATE schedule_notification SET is_scheduled = 1 WHERE id = :id")
    suspend fun updateScheduled(id: String)

    @Query("UPDATE schedule_notification SET is_shown = 1 WHERE id = :id")
    suspend fun updateShown(id: String)

    @Query("SELECT * FROM schedule_notification WHERE id = :notificationId")
    suspend fun getNotification(notificationId: String): ScheduleNotification

    @Query("UPDATE schedule_notification SET is_scheduled = 1 WHERE category = :category")
    suspend fun removeScheduledCategory(category: String)

    @Transaction
    suspend fun removeCategory(category: String): List<String> {
        removeScheduledCategory(category)
        return getIdFromCategory(category)
    }

}