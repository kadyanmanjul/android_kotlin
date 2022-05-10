package com.joshtalks.joshskills.voip.data.local

import android.content.Context
import androidx.room.*
import com.joshtalks.joshskills.voip.voipanalytics.data.local.VoipAnalyticsDao
import com.joshtalks.joshskills.voip.voipanalytics.data.local.VoipAnalyticsEntity

// TODO: Will be used to insert Disconnected call Data and Voip Analytics
const val PENDING = 0
const val SYNCED = 1

@Database(entities = [DisconnectCallEntity::class,VoipAnalyticsEntity::class], version = 3, exportSchema = true)
abstract class VoipDatabase : RoomDatabase() {
    abstract fun getDisconnectCallDao() : DisconnectCallDao
    abstract fun voipAnalyticsDao() : VoipAnalyticsDao

    companion object {

        @Volatile
        private var INSTANCE: VoipDatabase? = null

        fun getDatabase(context: Context): VoipDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoipDatabase::class.java,
                    "voip_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
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