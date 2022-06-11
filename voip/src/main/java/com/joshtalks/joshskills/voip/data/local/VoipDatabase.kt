package com.joshtalks.joshskills.voip.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.joshtalks.joshskills.voip.recordinganalytics.data.local.RecordingAnalyticsDao
import com.joshtalks.joshskills.voip.recordinganalytics.data.local.RecordingAnalyticsEntity
import com.joshtalks.joshskills.voip.voipanalytics.data.local.VoipAnalyticsDao
import com.joshtalks.joshskills.voip.voipanalytics.data.local.VoipAnalyticsEntity

// TODO: Will be used to insert Disconnected call Data and Voip Analytics
const val PENDING = 0
const val SYNCED = 1

@Database(entities = [DisconnectCallEntity::class,VoipAnalyticsEntity::class, RecordingAnalyticsEntity::class], version = 4, exportSchema = true)
abstract class VoipDatabase : RoomDatabase() {
    abstract fun getDisconnectCallDao() : DisconnectCallDao
    abstract fun voipAnalyticsDao() : VoipAnalyticsDao
    abstract fun recordingAnalyticsDao() : RecordingAnalyticsDao

    companion object {

        @Volatile
        private var INSTANCE: VoipDatabase? = null

        fun getDatabase(context: Context): VoipDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoipDatabase::class.java,
                    "voip_database"
                )//.addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE voip_analytics ADD COLUMN extra TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

@Entity(tableName = "voip_disconnect_table")
data class DisconnectCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelName: String,
    val mentorId: String,
    val duration: Long,
    val status : Int = PENDING
)

@Dao
interface DisconnectCallDao {
    @Insert
    suspend fun insertDisconnectedData(data: DisconnectCallEntity)

    @Query(value = "DELETE from voip_disconnect_table WHERE status = 1")
    suspend fun delete()
}