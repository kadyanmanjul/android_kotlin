package com.joshtalks.joshskills.core.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joshtalks.joshskills.core.notification.model.NotificationModel

@Dao
interface NotificationDao {

    //TODO: OnConflict REPLACE or FAIL?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationModel)

    @Query("SELECT * FROM notification_table WHERE analytics_sent = 0")
    suspend fun getUnsentAnalytics(): List<NotificationModel>

    @Query("DELETE from notification_table")
    suspend fun clearAllItems()
}