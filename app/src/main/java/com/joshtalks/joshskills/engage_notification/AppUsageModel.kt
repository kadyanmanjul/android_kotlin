package com.joshtalks.joshskills.engage_notification

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.util.Date

@Entity(tableName = "app_usage")
data class AppUsageModel(
    @ColumnInfo(name = "usage_time")
    var usageTime: Int
) {

    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0

    @ColumnInfo(name = "created")
    var usageDate: Date = Date()
}

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoAppUsage(obj: AppUsageModel)
}