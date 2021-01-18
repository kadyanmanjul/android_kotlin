package com.joshtalks.joshskills.engage_notification

import androidx.room.*
import com.google.gson.annotations.Expose
import java.util.*

@Entity(tableName = "app_usage")
data class AppUsageModel(
    @ColumnInfo(name = "usage_time")
    var usageTime: Long
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