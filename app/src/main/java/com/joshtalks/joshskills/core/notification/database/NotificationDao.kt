package com.joshtalks.joshskills.core.notification.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joshtalks.joshskills.core.notification.model.NotificationModel

@Dao
interface NotificationDao {

    //TODO: OnConflict REPLACE or FAIL?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationModel)

    @Query("SELECT * FROM notification_table WHERE analytics_sent = 0")
    suspend fun getUnsentAnalytics(): List<NotificationModel>?

    @Query("SELECT * FROM notification_table WHERE isSynced = 0 AND `action` LIKE :search ")
    suspend fun getUnsyncedNotifications(search : String): List<NotificationModel>?

    @Update
    fun updateNotificationObject(notification: NotificationModel)

    @Query("UPDATE notification_table SET `action` = :action , actionTime = :time   where id=:id")
    fun updateAction(id: String,action:String,time:Long = System.currentTimeMillis())

    @Query("UPDATE notification_table SET `analytics_sent` = 1 where id=:id")
    fun updateEngagementStatus(id: String)

    @Query("UPDATE notification_table SET `isSynced` = 1 where id=:id")
    fun updateSyncStatus(id: String)

    @Query("DELETE from notification_table")
    suspend fun clearAllItems()
}