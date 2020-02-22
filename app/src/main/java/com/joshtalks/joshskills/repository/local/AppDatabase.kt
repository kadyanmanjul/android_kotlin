package com.joshtalks.joshskills.repository.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.entity.*
import java.util.*


const val DATABASE_NAME = "JoshEnglishDB.db"

@Database(
    entities = [Course::class, ChatModel::class, Question::class, VideoType::class, AudioType::class, OptionType::class, PdfType::class, ImageType::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(
    MessageTypeConverters::class,
    ConvertersForDownloadStatus::class,
    ConvertersForUser::class,
    DateConverter::class,
    MessageDeliveryTypeConverter::class,
    MessageStatusTypeConverters::class,
    ExpectedEngageTypeConverter::class,
    ConvectorForEngagement::class

)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase? {
            if (null == INSTANCE) {
                synchronized(AppDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java, DATABASE_NAME
                        )
                            .addMigrations(
                                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                                MIGRATION_8_9
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build()
                    }
                }
            }
            return INSTANCE
        }

        private val sRoomDatabaseCallback = object : RoomDatabase.Callback() {

        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN course_icon TEXT")
                database.execSQL("ALTER TABLE chat_table ADD COLUMN is_delete_message INTEGER NOT NULL DEFAULT 0 ")
                database.execSQL("ALTER TABLE PdfTable ADD COLUMN thumbnail TEXT")
                database.execSQL("ALTER TABLE PdfTable ADD COLUMN size TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE PdfTable ADD COLUMN pages TEXT")
                database.execSQL("UPDATE chat_table  SET is_seen = 1")
            }
        }
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN download_progress INTEGER NOT NULL DEFAULT 0 ")
            }
        }
        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN status TEXT ")
            }
        }
        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN course_created_date INTEGER ")
            }
        }
        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN question_id INTEGER")
            }
        }
        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE question_table ADD COLUMN feedback_require TEXT")
                database.execSQL("ALTER TABLE question_table ADD COLUMN expectedEngageType TEXT")
                database.execSQL("ALTER TABLE question_table ADD COLUMN practice_engagements TEXT")
            }
        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN chat_type TEXT ")
                database.execSQL("ALTER TABLE question_table ADD COLUMN type TEXT ")
            }
        }
        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN chat_type TEXT ")
                database.execSQL("ALTER TABLE question_table ADD COLUMN type TEXT ")
            }
        }


        fun clearDatabase() {
            INSTANCE?.clearAllTables()
        }

    }

    abstract fun courseDao(): CourseDao
    abstract fun chatDao(): ChatDao

}

class MessageTypeConverters {
    @TypeConverter
    fun fromString(value: String?): BASE_MESSAGE_TYPE? {
        try {
            val matType = object : TypeToken<BASE_MESSAGE_TYPE>() {}.type
            return AppObjectController.gsonMapper.fromJson<BASE_MESSAGE_TYPE>(
                value ?: BASE_MESSAGE_TYPE.TX.name, matType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            return BASE_MESSAGE_TYPE.Q
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: BASE_MESSAGE_TYPE): String? {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}


class ConvertersForDownloadStatus {

    @TypeConverter
    fun fromString(value: String): DOWNLOAD_STATUS {
        val matType = object : TypeToken<DOWNLOAD_STATUS>() {}.type
        return AppObjectController.gsonMapper.fromJson<DOWNLOAD_STATUS>(value, matType)
    }

    @TypeConverter
    fun fromMatType(enumVal: DOWNLOAD_STATUS): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}


class ConvertersForUser {

    @TypeConverter
    fun fromStringToUser(value: String?): User? {
        val matType = object : TypeToken<User>() {}.type
        if (value == null) {
            return User()
        }
        return AppObjectController.gsonMapper.fromJson<User>(value, matType)
    }

    @TypeConverter
    fun fromUserToString(obj: User): String {
        return AppObjectController.gsonMapper.toJson(obj)
    }
}

class DateConverter {

    @TypeConverter
    fun toDate(dateLong: Long?): Date? {
        return if (dateLong == null) null else Date(dateLong)
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return (date?.time)
    }
}


class MessageDeliveryTypeConverter {
    @TypeConverter
    fun fromString(value: String?): MESSAGE_DELIVER_STATUS {
        val matType = object : TypeToken<MESSAGE_DELIVER_STATUS>() {}.type
        return AppObjectController.gsonMapper.fromJson<MESSAGE_DELIVER_STATUS>(
            value ?: MESSAGE_DELIVER_STATUS.READ.name, matType
        )
    }

    @TypeConverter
    fun fromMatType(enumVal: MESSAGE_DELIVER_STATUS): String? {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}


class MessageStatusTypeConverters {
    @TypeConverter
    fun fromString(value: String?): MESSAGE_STATUS? {
        val matType = object : TypeToken<MESSAGE_STATUS>() {}.type
        return AppObjectController.gsonMapper.fromJson<MESSAGE_STATUS>(
            value?.replace("\"", EMPTY) ?: MESSAGE_STATUS.SEEN_BY_SERVER.name, matType
        )
    }

    @TypeConverter
    fun fromMatType(enumVal: MESSAGE_STATUS?): String? {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}


class ExpectedEngageTypeConverter {
    @TypeConverter
    fun fromString(value: String?): EXPECTED_ENGAGE_TYPE? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val matType = object : TypeToken<EXPECTED_ENGAGE_TYPE>() {}.type
        return AppObjectController.gsonMapper.fromJson<EXPECTED_ENGAGE_TYPE>(
            value, matType
        )
    }

    @TypeConverter
    fun fromType(enumVal: EXPECTED_ENGAGE_TYPE?): String? {
        if (enumVal == null) {
            return null
        }
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}


class ConvectorForEngagement {
    @TypeConverter
    fun fromEngagement(value: List<PracticeEngagement>?): String {
        val type = object : TypeToken<List<PracticeEngagement>>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toEngagement(value: String?): List<PracticeEngagement> {
        if (value == null) {
            return Collections.emptyList()
        }
        val type = object : TypeToken<List<PracticeEngagement>>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}


