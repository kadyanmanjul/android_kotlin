package com.joshtalks.joshskills.common.core.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.common.core.notification.model.NotificationEvent

@Dao
interface NotificationEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationEvent(notification: NotificationEvent)

    @Query("SELECT * FROM notification_event_table WHERE id = :id")
    suspend fun getNotificationEvent(id: String): List<NotificationEvent>?

    @Query("SELECT * FROM notification_event_table WHERE analytics_sent = 0 LIMIT 30")
    suspend fun getUnsyncEvent(): List<NotificationEvent>?

    @Query("UPDATE notification_event_table SET analytics_sent = 1 WHERE notificationId = :id")
    suspend fun updateSyncStatus(id: Int)

    @Query("DELETE FROM notification_event_table")
    suspend fun clearEventsData()

}