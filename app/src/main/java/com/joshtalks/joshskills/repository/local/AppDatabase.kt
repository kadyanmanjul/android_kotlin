package com.joshtalks.joshskills.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.engage_notification.AppActivityDao
import com.joshtalks.joshskills.engage_notification.AppActivityModel
import com.joshtalks.joshskills.engage_notification.AppUsageDao
import com.joshtalks.joshskills.engage_notification.AppUsageModel
import com.joshtalks.joshskills.repository.local.dao.AssessmentDao
import com.joshtalks.joshskills.repository.local.dao.LessonDao
import com.joshtalks.joshskills.repository.local.dao.PendingTaskDao
import com.joshtalks.joshskills.repository.local.dao.reminder.ReminderDao
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModel
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModelDao
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatDao
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.CourseDao
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.FeedbackEngageModel
import com.joshtalks.joshskills.repository.local.entity.FeedbackEngageModelDao
import com.joshtalks.joshskills.repository.local.entity.ImageType
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.entity.NPSEventModelDao
import com.joshtalks.joshskills.repository.local.entity.OptionType
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.User
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.local.entity.VideoEngageDao
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentMediaType
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentStatus
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentType
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterChoiceColumn
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterChoiceType
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterQuestionStatus
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.assessment.AssessmentIntro
import com.joshtalks.joshskills.repository.server.assessment.ReviseConcept
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import java.util.Collections
import java.util.Date


const val DATABASE_NAME = "JoshEnglishDB.db"

@Database(
    entities = [Course::class, ChatModel::class, Question::class, VideoType::class,
        AudioType::class, OptionType::class, PdfType::class, ImageType::class, VideoEngage::class,
        FeedbackEngageModel::class, NPSEventModel::class, Assessment::class, AssessmentQuestion::class,
        Choice::class, ReviseConcept::class, AssessmentIntro::class, ReminderResponse::class,
        AppUsageModel::class, AppActivityModel::class, LessonModel::class, PendingTaskModel::class, AwardMentorModel::class
    ],
    version = 26,
    exportSchema = true
)
@TypeConverters(
    MessageTypeConverters::class,
    ConvertersForDownloadStatus::class,
    ConvertersForUser::class,
    DateConverter::class,
    MessageDeliveryTypeConverter::class,
    MessageStatusTypeConverters::class,
    ExpectedEngageTypeConverter::class,
    ConvectorForEngagement::class,
    ConvectorForGraph::class,
    ConvectorForNPSEvent::class,
    TypeConverterAssessmentStatus::class,
    TypeConverterChoiceType::class,
    TypeConverterQuestionStatus::class,
    TypeConverterChoiceColumn::class,
    TypeConverterAssessmentType::class,
    TypeConverterAssessmentMediaType::class,
    ChatTypeConverters::class,
    LessonStatus::class,
    ConvectorForPracticeFeedback::class,
    QuestionStatus::class,
    CExamStatusTypeConverter::class,
    PendingTaskTypeConverter::class,
    PendingTaskRequestTypeConverter::class
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
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                                MIGRATION_8_9,
                                MIGRATION_9_10,
                                MIGRATION_10_11,
                                MIGRATION_11_12,
                                MIGRATION_12_13,
                                MIGRATION_13_14,
                                MIGRATION_14_16,
                                MIGRATION_16_17,
                                MIGRATION_17_18,
                                MIGRATION_18_19,
                                MIGRATION_19_20,
                                MIGRATION_20_21,
                                MIGRATION_21_22,
                                MIGRATION_22_23,
                                MIGRATION_23_24,
                                MIGRATION_24_25,
                                MIGRATION_25_26
                            )
                            //  .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
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
                database.execSQL("ALTER TABLE question_table ADD COLUMN type TEXT NOT NULL DEFAULT 'Q' ")
            }
        }
        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    val chatTypeQuery = "SELECT chat_type FROM course"
                    if (existsColumnInTable(database, chatTypeQuery).not()) {
                        database.execSQL("ALTER TABLE course ADD COLUMN chat_type TEXT")
                    }
                    val typeQuery = "SELECT type FROM question_table"
                    if (existsColumnInTable(database, typeQuery).not()) {
                        database.execSQL("ALTER TABLE question_table ADD COLUMN type TEXT NOT NULL DEFAULT 'Q'")
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    val cursor = database.query("select conversation_id from course")
                    if (cursor.moveToFirst()) {
                        val key = cursor.getString(cursor.getColumnIndex("conversation_id"))
                        PrefManager.removeKey(key)
                    }
                    cursor.close()

                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN content_download_date INTEGER NOT NULL DEFAULT 0 ")
            }
        }
        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE question_table ADD COLUMN practice_no INTEGER")
            }
        }

        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN message_time_in_milliSeconds TEXT  NOT NULL DEFAULT '' ")
            }
        }

        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN report_status INTEGER NOT NULL DEFAULT 0 ")
            }
        }
        private val MIGRATION_14_16: Migration = object : Migration(14, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE question_table ADD COLUMN need_feedback INTEGER ")
                    database.execSQL("ALTER TABLE question_table ADD COLUMN upload_feedback_status INTEGER NOT NULL DEFAULT 0 ")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `video_watch_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mentorId` TEXT, `gID` TEXT, `graph` TEXT NOT NULL, `videoId` INTEGER NOT NULL, `watchTime` INTEGER NOT NULL)")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `feedback_engage` (`id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        private val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE question_table ADD COLUMN interval INTEGER NOT NULL DEFAULT -1")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `nps_event_table` (`id` INTEGER NOT NULL, `day` INTEGER NOT NULL, `enable` INTEGER NOT NULL, `event` TEXT NOT NULL, `event_name` TEXT NOT NULL, `event_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, PRIMARY KEY(`day`, `event_id`, `event_name`))")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        private val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE question_table ADD COLUMN conversation_practice_id TEXT ")
                database.execSQL("ALTER TABLE question_table ADD COLUMN assessment_id INTEGER")
                database.execSQL("ALTER TABLE question_table ADD COLUMN temp_type TEXT")

                database.execSQL("ALTER TABLE VideoTable ADD COLUMN interval INTEGER NOT NULL DEFAULT -1")

                database.execSQL("CREATE TABLE IF NOT EXISTS assessments (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `icon_url` TEXT,`text1` TEXT, `text2` TEXT, `score_text` TEXT, `heading` TEXT NOT NULL, `title` TEXT, `imageUrl` TEXT, `description` TEXT, `type` TEXT NOT NULL, `status` TEXT NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessments_remoteId` ON assessments (`remoteId`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS assessment_questions (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `assessmentId` INTEGER NOT NULL, `text` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `mediaUrl` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `videoThumbnailUrl` TEXT, `choiceType` TEXT NOT NULL, `isAttempted` INTEGER NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`assessmentId`) REFERENCES `assessments`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_questions_assessmentId` ON `assessment_questions` (`assessmentId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_questions_remoteId` ON `assessment_questions` (`remoteId`)")

                database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_choice` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `questionId` INTEGER NOT NULL, `text` TEXT, `imageUrl` TEXT, `isCorrect` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `correctAnswerOrder` INTEGER NOT NULL, `column` TEXT NOT NULL, `userSelectedOrder` INTEGER NOT NULL, `isSelectedByUser` INTEGER NOT NULL, FOREIGN KEY(`questionId`) REFERENCES `assessment_questions`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_choice_questionId` ON `assessment_choice` (`questionId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_choice_remoteId` ON `assessment_choice` (`remoteId`)")


                database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_revise_concept` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `questionId` INTEGER NOT NULL, `heading` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `mediaUrl` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `videoThumbnailUrl` TEXT, FOREIGN KEY(`questionId`) REFERENCES `assessment_questions`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_revise_concept_questionId` ON `assessment_revise_concept` (`questionId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_revise_concept_localId` ON `assessment_revise_concept` (`localId`)")


                database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_intro` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `assessmentId` INTEGER NOT NULL, `title` TEXT, `description` TEXT, `imageUrl` TEXT, FOREIGN KEY(`assessmentId`) REFERENCES `assessments`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_intro_assessmentId` ON `assessment_intro` (`assessmentId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_intro_localId` ON `assessment_intro` (`localId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_intro_type` ON `assessment_intro` (`type`)")

            }
        }

        private val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN last_use_time INTEGER DEFAULT '" + Date().time + "'")
                database.execSQL("CREATE TABLE IF NOT EXISTS `reminder_table` (`reminder_id` INTEGER PRIMARY KEY NOT NULL, `mentor_id` TEXT NOT NULL, `reminder_frequency` TEXT NOT NULL, `status` TEXT NOT NULL, `reminder_time` TEXT NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_table_reminder_id` ON `reminder_table` (`reminder_id`)")
            }
        }

        private val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN batch_started TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN voicecall_status INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        private val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE video_watch_table ADD COLUMN is_sync INTEGER NOT NULL DEFAULT 0 ")
                database.execSQL("ALTER TABLE video_watch_table ADD COLUMN course_id INTEGER NOT NULL DEFAULT -1 ")
            }
        }
        private val MIGRATION_22_23: Migration = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN is_group_active INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `chat_table` ADD COLUMN lesson_id INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE `question_table` ADD COLUMN lesson_id INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN practice_word TEXT ")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN status TEXT NOT NULL DEFAULT 'NA'")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN lesson_status TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN chat_type TEXT DEFAULT 'OTHER'")
                //   database.execSQL("ALTER TABLE `question_table` ADD COLUMN lesson INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE question_table ADD COLUMN certificate_exam_id INTEGER")
                database.execSQL("ALTER TABLE question_table ADD COLUMN topic_id TEXT ")


                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_attemptLeft INTEGER")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_attemptOn TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_attempted INTEGER")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_batchIcon TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_code TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_eligibilityDate TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_marks REAL")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_passedOn TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_examStatus TEXT")
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN cexam_text TEXT")

                database.execSQL("CREATE TABLE IF NOT EXISTS `app_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created` INTEGER NOT NULL, `usage_time` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `app_activity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created` INTEGER NOT NULL, `activity` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `lessonmodel` (`lesson_id` INTEGER NOT NULL, `lesson_no` INTEGER NOT NULL, `lesson_name` TEXT NOT NULL, `thumbnail` TEXT NOT NULL, `status` TEXT, `course` INTEGER NOT NULL, `interval` INTEGER NOT NULL, PRIMARY KEY(`lesson_id`))")
            }
        }

        private val MIGRATION_23_24: Migration = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `grammarStatus` TEXT")
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `vocabularyStatus` TEXT")
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `readingStatus` TEXT")
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `speakingStatus` TEXT")
                database.execSQL("CREATE TABLE IF NOT EXISTS `pending_task_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `request_object` TEXT NOT NULL, `type` TEXT NOT NULL, `retry_count` INTEGER NOT NULL)")
            }

        }
        private val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `question_table` ADD COLUMN `vp_sort_order` INTEGER NOT NULL DEFAULT -1")
            }

        }
        private val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `chat_table` ADD COLUMN award_mentor_id INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE TABLE IF NOT EXISTS `awardmentormodel` (`id` INTEGER NOT NULL, `award_image_url` TEXT, `award_text` TEXT, `description` TEXT, `performer_name` TEXT, `performer_photo_url` TEXT, `total_points_text` TEXT, PRIMARY KEY(`id`))")
            }
        }

        fun clearDatabase() {
            INSTANCE?.clearAllTables()
        }

        fun existsColumnInTable(
            database: SupportSQLiteDatabase,
            query: String
        ): Boolean {
            try {
                database.query(query)
                return true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return false


        }

    }

    abstract fun courseDao(): CourseDao
    abstract fun chatDao(): ChatDao
    abstract fun videoEngageDao(): VideoEngageDao
    abstract fun feedbackEngageModelDao(): FeedbackEngageModelDao
    abstract fun npsEventModelDao(): NPSEventModelDao
    abstract fun assessmentDao(): AssessmentDao
    abstract fun reminderDao(): ReminderDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun appActivityDao(): AppActivityDao
    abstract fun lessonDao(): LessonDao
    abstract fun awardMentorModelDao(): AwardMentorModelDao
    abstract fun pendingTaskDao(): PendingTaskDao

}

class MessageTypeConverters {
    @TypeConverter
    fun fromString(value: String?): BASE_MESSAGE_TYPE? {
        return try {
            val matType = object : TypeToken<BASE_MESSAGE_TYPE>() {}.type
            AppObjectController.gsonMapper.fromJson(
                value ?: BASE_MESSAGE_TYPE.TX.name, matType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            BASE_MESSAGE_TYPE.Q
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: BASE_MESSAGE_TYPE?): String? {
        if (null != enumVal) {
            return AppObjectController.gsonMapper.toJson(enumVal)
        }
        return AppObjectController.gsonMapper.toJson(BASE_MESSAGE_TYPE.OTHER)
    }
}


class ConvertersForDownloadStatus {

    @TypeConverter
    fun fromString(value: String): DOWNLOAD_STATUS {
        val matType = object : TypeToken<DOWNLOAD_STATUS>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, matType)
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
        return AppObjectController.gsonMapper.fromJson(
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


class ConvectorForGraph {
    @TypeConverter
    fun fromGraph(value: List<Graph>?): String {
        val type = object : TypeToken<List<Graph>>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toGraph(value: String?): List<Graph> {
        if (value == null) {
            return Collections.emptyList()
        }
        val type = object : TypeToken<List<Graph>>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}


class ConvectorForPracticeFeedback {
    @TypeConverter
    fun fromPracticeFeedback(value: PracticeFeedback?): String {
        val type = object : TypeToken<PracticeFeedback>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toPracticeFeedback(value: String?): PracticeFeedback {
        val type = object : TypeToken<PracticeFeedback>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}


class ConvectorForNPSEvent {
    @TypeConverter
    fun fromNPSEvent(value: NPSEvent): String {
        val type = object : TypeToken<NPSEvent>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toNPSEvent(value: String): NPSEvent {
        val type = object : TypeToken<NPSEvent>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}


class ChatTypeConverters {
    @TypeConverter
    fun fromString(value: String?): CHAT_TYPE {
        return try {
            val matType = object : TypeToken<CHAT_TYPE>() {}.type
            AppObjectController.gsonMapper.fromJson(
                value ?: CHAT_TYPE.OTHER.name, matType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            CHAT_TYPE.OTHER
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: CHAT_TYPE?): String {
        if (null != enumVal) {
            return AppObjectController.gsonMapper.toJson(enumVal)
        }
        return AppObjectController.gsonMapper.toJson(CHAT_TYPE.OTHER)
    }
}


class LessonStatus {
    @TypeConverter
    fun fromString(value: String?): LESSON_STATUS? {
        return try {
            val matType = object : TypeToken<LESSON_STATUS>() {}.type
            AppObjectController.gsonMapper.fromJson(
                value ?: LESSON_STATUS.NO.name, matType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            LESSON_STATUS.NO
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: LESSON_STATUS?): String? {
        if (null != enumVal) {
            return AppObjectController.gsonMapper.toJson(enumVal)
        }
        return AppObjectController.gsonMapper.toJson(LESSON_STATUS.NO)
    }
}

class QuestionStatus {
    @TypeConverter
    fun fromString(value: String?): QUESTION_STATUS? {
        return try {
            val matType = object : TypeToken<QUESTION_STATUS>() {}.type
            AppObjectController.gsonMapper.fromJson(
                value ?: QUESTION_STATUS.NA.name, matType
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            QUESTION_STATUS.NA
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: QUESTION_STATUS?): String? {
        if (null != enumVal) {
            return AppObjectController.gsonMapper.toJson(enumVal)
        }
        return AppObjectController.gsonMapper.toJson(QUESTION_STATUS.NA)
    }
}

class CExamStatusTypeConverter {
    @TypeConverter
    fun fromString(value: String): CExamStatus {
        val type = object : TypeToken<CExamStatus>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromMatType(enumVal: CExamStatus): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}

class PendingTaskTypeConverter {
    @TypeConverter
    fun fromString(value: String): PendingTask {
        val type = object : TypeToken<PendingTask>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromMatType(enumVal: PendingTask): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}

class PendingTaskRequestTypeConverter {
    @TypeConverter
    fun fromString(value: String): RequestEngage {
        val type = object : TypeToken<RequestEngage>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromMatType(enumVal: RequestEngage): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}





