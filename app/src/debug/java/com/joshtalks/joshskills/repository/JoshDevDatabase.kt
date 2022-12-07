package com.joshtalks.joshskills.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.joshtalks.joshskills.repository.dao.ApiRequestDao
import com.joshtalks.joshskills.repository.entity.ApiRequest

@Database(entities = [ApiRequest::class], version = 1, exportSchema = false)
abstract class JoshDevDatabase : RoomDatabase() {
    companion object {
        private const val DATABASE_NAME = "josh_dev_database"

        @Volatile
        private var INSTANCE: JoshDevDatabase? = null

        fun getDatabase(context: Context): JoshDevDatabase? {
            if (null == INSTANCE) {
                synchronized(JoshDevDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            JoshDevDatabase::class.java, DATABASE_NAME
                        ).fallbackToDestructiveMigration()
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build()
                    }
                }
            }
            return INSTANCE
        }

        fun clearDatabase() {
            INSTANCE?.clearAllTables()
        }
    }

    abstract fun apiRequestDao(): ApiRequestDao
}