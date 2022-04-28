package com.joshtalks.joshskills.voip.data.local

import android.content.Context
import androidx.room.*

// TODO: Will be used to insert Disconnected call Data and Voip Analytics
const val PENDING = 0
const val SYNCED = 1

@Database(entities = [DisconnectCallEntity::class], version = 1, exportSchema = true)
abstract class VoipDatabase : RoomDatabase() {
    abstract fun getDisconnectCallDao() : DisconnectCallDao

    companion object {

        @Volatile
        private var INSTANCE: VoipDatabase? = null

        fun getDatabase(context: Context): VoipDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoipDatabase::class.java,
                    "voip_database"
                ).build()
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