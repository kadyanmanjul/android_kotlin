package com.joshtalks.joshskills.engage_notification

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.time.Instant

@Entity(tableName = "app_usage")
data class AppUsageModel(
    @SerializedName("miliseconds")
    @ColumnInfo(name = "usage_time")
    var usageTime: Long
) {

    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0

    @SerializedName("created")
    @ColumnInfo(name = "created")
    var usageDate: Long = Instant.now().epochSecond

    @Ignore
    @SerializedName("mentor_id")
    var mentorId: String? = null

    @Ignore
    @SerializedName("gaid_id")
    var gaidId: String? = null
}

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoAppUsage(obj: AppUsageModel)

    @Query(value = "SELECT * FROM app_usage ")
    suspend fun getAllSession(): List<AppUsageModel>

    @Query("DELETE FROM app_usage")
    suspend fun deleteAllSyncSession()
}
