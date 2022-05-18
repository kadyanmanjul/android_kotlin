package com.joshtalks.joshskills.core.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joshtalks.joshskills.core.notification.model.NotificationEvent
import com.joshtalks.joshskills.core.notification.model.NotificationModel

@Dao
interface NotificationEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationEvent(notification: NotificationEvent)

    @Query("SELECT * FROM notification_event_table WHERE id = :id")
    suspend fun getNotificationEvent(id:String): List<NotificationEvent>?

    @Query("SELECT * FROM notification_event_table WHERE analytics_sent = 0")
    suspend fun getUnsyncEvent(): List<NotificationEvent>?

    @Query("UPDATE notification_event_table SET analytics_sent = 1 WHERE notificationId = :id")
    suspend fun updateSyncStatus(id:Int)

}