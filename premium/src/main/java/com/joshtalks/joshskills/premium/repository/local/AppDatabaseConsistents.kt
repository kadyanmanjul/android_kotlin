package com.joshtalks.joshskills.premium.repository.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.BranchLog
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.BranchLogDao

/**
 * Will be used to create consistans table and this will not clear
 */

@Database(entities = [BranchLog::class], version = 1, exportSchema = true)
abstract class AppDatabaseConsistents : RoomDatabase() {
    abstract fun branchLogDao() : BranchLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabaseConsistents? = null

        fun getDatabase(context: Context): AppDatabaseConsistents {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabaseConsistents::class.java,
                    "app_database_consistents"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `branch_log_table` (`amount` REAL NOT NULL, `course_name` TEXT NOT NULL,`test_id` TEXT NOT NULL,`order_id` TEXT NOT NULL, `is_sync` INTEGER NOT NULL, PRIMARY KEY(`order_id`))")
            }
        }
    }
}