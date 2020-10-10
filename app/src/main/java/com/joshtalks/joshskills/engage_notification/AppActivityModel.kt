package com.joshtalks.joshskills.engage_notification


import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import java.util.Date

@Entity(tableName = "app_activity")
data class AppActivityModel(
    @ColumnInfo(name = "activity")
    var activityName: String
) {

    @PrimaryKey(autoGenerate = true)
    @Expose
    var id: Long = 0

    @ColumnInfo(name = "created")
    var usageDate: Date = Date()
}

@Dao
interface AppActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIntoAppActivity(obj: AppActivityModel)
}