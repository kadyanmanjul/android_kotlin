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
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.VariableMap
import com.joshtalks.joshskills.core.abTest.repository.ABTestCampaignDao
import com.joshtalks.joshskills.engage_notification.AppActivityDao
import com.joshtalks.joshskills.engage_notification.AppActivityModel
import com.joshtalks.joshskills.engage_notification.AppUsageDao
import com.joshtalks.joshskills.engage_notification.AppUsageModel
import com.joshtalks.joshskills.quizgame.analytics.data.GameAnalyticsDao
import com.joshtalks.joshskills.quizgame.analytics.data.GameAnalyticsEntity
import com.joshtalks.joshskills.repository.local.dao.AssessmentDao
import com.joshtalks.joshskills.repository.local.dao.ChatDao
import com.joshtalks.joshskills.repository.local.dao.CommonDao
import com.joshtalks.joshskills.repository.local.dao.LessonDao
import com.joshtalks.joshskills.repository.local.dao.PendingTaskDao
import com.joshtalks.joshskills.repository.local.dao.reminder.ReminderDao
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModel
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModelDao
import com.joshtalks.joshskills.repository.local.entity.AwardTypes
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.entity.CourseDao
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.FeedbackEngageModel
import com.joshtalks.joshskills.repository.local.entity.FeedbackEngageModelDao
import com.joshtalks.joshskills.repository.local.entity.ImageType
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionDao
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
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
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.User
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.local.entity.VideoEngageDao
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearch
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearchDao
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCallerDao
import com.joshtalks.joshskills.repository.local.entity.practise.Phonetic
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementDao
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.entity.practise.WrongWord
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionFeedback
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
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopicDao
import com.joshtalks.joshskills.track.CourseUsageDao
import com.joshtalks.joshskills.track.CourseUsageModel
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupChatAnalyticsEntity
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupsAnalyticsDao
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupsAnalyticsEntity
import com.joshtalks.joshskills.ui.group.data.local.GroupChatDao
import com.joshtalks.joshskills.ui.group.data.local.GroupListDao
import com.joshtalks.joshskills.ui.group.data.local.TimeTokenDao
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.model.TimeTokenRequest
import com.joshtalks.joshskills.ui.special_practice.model.SpecialDao
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsDao
import com.joshtalks.joshskills.ui.voip.analytics.data.local.VoipAnalyticsEntity
import java.math.BigDecimal
import java.util.Collections
import java.util.Date

const val DATABASE_NAME = "JoshEnglishDB.db"

@Database(
    entities = [
        Course::class, ChatModel::class, Question::class, VideoType::class,
        AudioType::class, OptionType::class, PdfType::class, ImageType::class, VideoEngage::class,
        FeedbackEngageModel::class, NPSEventModel::class, Assessment::class, AssessmentQuestion::class,
        Choice::class, ReviseConcept::class, AssessmentIntro::class, ReminderResponse::class,
        AppUsageModel::class, AppActivityModel::class, LessonModel::class, PendingTaskModel::class,
        PracticeEngagementV2::class, AwardMentorModel::class, LessonQuestion::class, SpeakingTopic::class,
        RecentSearch::class, FavoriteCaller::class, CourseUsageModel::class, AssessmentQuestionFeedback::class,
        VoipAnalyticsEntity::class, GroupsAnalyticsEntity::class, GroupChatAnalyticsEntity::class,
        GroupsItem::class, TimeTokenRequest::class, ChatItem::class, GameAnalyticsEntity::class, SpecialPractice::class, ABTestCampaignData::class
    ],
    version = 46,
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
    ConvertorForEngagement::class,
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
    QuestionStatus::class,
    CExamStatusTypeConverter::class,
    PendingTaskTypeConverter::class,
    PendingTaskRequestTypeConverter::class,
    ListConverters::class,
    ConvectorForWrongWord::class,
    ConvectorForPhoneticClass::class,
    ConverterForLessonQuestionType::class,
    ConverterForLessonMaterialType::class,
    AwardTypeConverter::class,
    BigDecimalConverters::class,
    VariableMapConverters::class
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
                                MIGRATION_25_26,
                                MIGRATION_26_27,
                                MIGRATION_27_28,
                                MIGRATION_28_29,
                                MIGRATION_30_31,
                                MIGRATION_31_32,
                                MIGRATION_32_33,
                                MIGRATION_33_34,
                                MIGRATION_34_35,
                                MIGRATION_35_36,
                                MIGRATION_36_37,
                                MIGRATION_37_38,
                                MIGRATION_38_39,
                                MIGRATION_39_40,
                                MIGRATION_40_41,
                                MIGRATION_41_42,
                                MIGRATION_42_43,
                                MIGRATION_43_44,
                                MIGRATION_44_45,
                                MIGRATION_45_46
                            )
                            .fallbackToDestructiveMigration()
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
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `attempt_number` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE TABLE IF NOT EXISTS `practise_engagement_table` (`questionForId` TEXT, `practiseId` INTEGER NOT NULL, `question` INTEGER NOT NULL, `answerUrl` TEXT NOT NULL, `duration` INTEGER NOT NULL, `practiceDate` TEXT NOT NULL, `feedbackRequire` TEXT, `text` TEXT, `localPath` TEXT, `transcriptId` TEXT, `pointsList` TEXT NOT NULL, `uploadStatus` TEXT NOT NULL, `feedback_feedbackId` INTEGER, `feedback_feedbackTitle` TEXT, `feedback_feedbackText` TEXT, `feedback_studentAudioUrl` TEXT, `feedback_teacherAudioUrl` TEXT, `feedback_wrong_word_list` TEXT, `feedback_created` INTEGER, `feedback_error` INTEGER, `feedback_pro_text` TEXT, `feedback_pro_description` TEXT, `feedback_rec_text` TEXT, `feedback_spd_text` TEXT, `feedback_spd_description` TEXT, PRIMARY KEY(`practiseId`), FOREIGN KEY(`questionForId`) REFERENCES `question_table`(`questionId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_practise_engagement_table_practiseId_question` ON `practise_engagement_table` (`practiseId`, `question`)")
                database.execSQL("ALTER TABLE course ADD COLUMN is_points_active INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_26_27: Migration = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE app_usage")
                database.execSQL("CREATE TABLE IF NOT EXISTS `app_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `created` INTEGER NOT NULL, `usage_time` INTEGER NOT NULL)")
                database.execSQL("ALTER TABLE `awardmentormodel` ADD COLUMN mentor_id TEXT")
            }
        }
        private val MIGRATION_27_28: Migration = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `awardmentormodel` ADD COLUMN award_type TEXT NOT NULL DEFAULT 'SOTD'")
                database.execSQL("ALTER TABLE `awardmentormodel` ADD COLUMN date_text TEXT")
            }
        }
        private val MIGRATION_28_29: Migration = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("UPDATE chat_table SET is_seen =1")
                // Db migration for course id
                database.execSQL("CREATE TABLE IF NOT EXISTS `RecentSearch` (`keyword` TEXT PRIMARY KEY NOT NULL)")
            }
        }
        private val MIGRATION_30_31: Migration = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN `award_user_id` INTEGER")
                database.execSQL("UPDATE chat_table SET award_user_id= award_mentor_id ")
                database.execSQL("UPDATE chat_table SET award_mentor_id= 0 ")
                database.execSQL("CREATE TABLE IF NOT EXISTS `favorite_caller` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `photo_url` TEXT, `minutes_spoken` INTEGER NOT NULL, `total_calls` INTEGER NOT NULL, `last_called_at` INTEGER NOT NULL, `is_deleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        private val MIGRATION_31_32: Migration = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `course_usage` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `start_time` INTEGER NOT NULL, `end_time` INTEGER, `conversation_id` TEXT NOT NULL, `created` INTEGER NOT NULL, `screen_name` TEXT)")
            }
        }
        private val MIGRATION_32_33: Migration = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.delete("favorite_caller", null, null)
                database.execSQL("ALTER TABLE favorite_caller ADD COLUMN mentor_id TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_33_34: Migration = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_practise_engagement_table_practiseId_question_questionForId` ON `practise_engagement_table` (`practiseId`, `question`, `questionForId`)")
                database.execSQL("DROP INDEX IF EXISTS `index_practise_engagement_table_practiseId_question`")
            }
        }

        private val MIGRATION_34_35: Migration = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE assessment_questions ADD COLUMN subText TEXT ")
                database.execSQL("ALTER TABLE assessment_questions ADD COLUMN mediaUrl2 TEXT ")
                database.execSQL("ALTER TABLE assessment_questions ADD COLUMN isNewHeader INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE assessment_questions ADD COLUMN mediaType2 TEXT NOT NULL DEFAULT 'NONE' ")
                database.execSQL("ALTER TABLE assessment_questions ADD COLUMN listOfAnswers TEXT ")
                database.execSQL("ALTER TABLE course ADD COLUMN is_course_locked INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE assessment_choice ADD COLUMN audioUrl TEXT ")
                database.execSQL("ALTER TABLE assessment_choice ADD COLUMN localAudioUrl TEXT ")
                database.execSQL("ALTER TABLE assessment_choice ADD COLUMN downloadStatus TEXT ")
                database.execSQL("ALTER TABLE lessonmodel ADD COLUMN show_new_grammar_screen INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_question_feedback` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `questionId` INTEGER NOT NULL, `correctAnswerHeading` TEXT, `correctAnswerText` TEXT, `wrongAnswerHeading` TEXT, `wrongAnswerText` TEXT, `wrongAnswerHeading2` TEXT, `wrongAnswerText2` TEXT, FOREIGN KEY(`questionId`) REFERENCES `assessment_questions`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_question_feedback_questionId` ON `assessment_question_feedback` (`questionId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_question_feedback_localId` ON `assessment_question_feedback` (`localId`)")
            }
        }

        private val MIGRATION_35_36: Migration = object : Migration(35, 36) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `voip_analytics` (`event` TEXT NOT NULL, `agoraCallId` TEXT NOT NULL, `agoraMentorUid` TEXT NOT NULL, `timeStamp` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            }
        }

        private val MIGRATION_36_37: Migration = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE SpeakingTopic ADD COLUMN `total_new_student_calls` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE SpeakingTopic ADD COLUMN  `required_new_student_calls` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE SpeakingTopic ADD COLUMN  `is_new_student_calls_activated` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_37_38: Migration = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN expire_date INTEGER")
                database.execSQL("ALTER TABLE course ADD COLUMN is_course_bought INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_38_39: Migration = object : Migration(38, 39) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `chat_table` ADD COLUMN sharableVideoDownloadedLocalPath TEXT ")
            }
        }

        private val MIGRATION_39_40: Migration = object : Migration(39, 40) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `groups_analytics` (`event` TEXT NOT NULL, `mentorId` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")

            }
        }
        private val MIGRATION_40_41: Migration = object : Migration(40, 41) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lesson_question ADD COLUMN conversation_question_id INTEGER ")
                database.execSQL("ALTER TABLE `lessonmodel` ADD COLUMN `conversationStatus` TEXT")

            }
        }
        private val MIGRATION_41_42: Migration = object : Migration(41, 42) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_table ADD COLUMN video_id INTEGER ")
                database.execSQL("ALTER TABLE video_watch_table ADD COLUMN is_sharable_video INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        private val MIGRATION_42_43: Migration = object : Migration(42, 43) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `group_list_table` (`groupId` TEXT PRIMARY KEY NOT NULL, `lastMessage` TEXT, `lastMsgTime` INTEGER NOT NULL, `unreadCount` TEXT, `adminId` TEXT, `groupIcon` TEXT, `createdAt` INTEGER, `name` TEXT, `createdBy` TEXT, `totalCalls` TEXT)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `time_token_db` (`groupId` TEXT PRIMARY KEY NOT NULL, `mentorId` TEXT NOT NULL, `timeToken` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `group_chat_db` (`messageId` TEXT PRIMARY KEY NOT NULL, `sender` TEXT, `message` TEXT NOT NULL, `msgTime` INTEGER NOT NULL, `groupId` TEXT NOT NULL, `msgType` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `group_chat_analytics` (`groupId` TEXT PRIMARY KEY NOT NULL, `lastSentMsgTime` INTEGER NOT NULL)")
                database.execSQL("ALTER TABLE `groups_analytics` ADD COLUMN `groupId` TEXT")
                database.execSQL("CREATE TABLE IF NOT EXISTS `game_analytics` (`event` TEXT NOT NULL, `mentorId` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            }
        }

        private val MIGRATION_43_44:Migration = object : Migration(43, 44) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `assessment_questions_tmp` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER NOT NULL, `assessmentId` INTEGER NOT NULL, `text` TEXT, `subText` TEXT, `sortOrder` INTEGER NOT NULL, `mediaUrl` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `mediaUrl2` TEXT, `mediaType2` TEXT NOT NULL, `videoThumbnailUrl` TEXT, `choiceType` TEXT NOT NULL, `isAttempted` INTEGER NOT NULL, `isNewHeader` INTEGER NOT NULL, `listOfAnswers` TEXT, `status` TEXT NOT NULL, FOREIGN KEY(`assessmentId`) REFERENCES `assessments`(`remoteId`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("INSERT INTO `assessment_questions_tmp` (`localId`,`remoteId`,`assessmentId`,`text`,`subText`,`sortOrder`,`mediaUrl`,`mediaType`,`mediaUrl2`,`mediaType2`,`videoThumbnailUrl`,`choiceType`,`isAttempted`,`isNewHeader`,`listOfAnswers`,`status`) SELECT `localId`,`remoteId`,`assessmentId`,`text`,`subText`,`sortOrder`,`mediaUrl`,`mediaType`,`mediaUrl2`,`mediaType2`,`videoThumbnailUrl`,`choiceType`,`isAttempted`,`isNewHeader`,`listOfAnswers`,`status` FROM `assessment_questions`")
                database.execSQL("DROP TABLE `assessment_questions`")
                database.execSQL("ALTER TABLE `assessment_questions_tmp` RENAME TO `assessment_questions`")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_assessment_questions_assessmentId` ON `assessment_questions` (`assessmentId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_assessment_questions_remoteId` ON `assessment_questions` (`remoteId`)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `ab_test_campaigns` (`is_campaign_active` INTEGER NOT NULL, `campaign_key` TEXT NOT NULL, `variant_key` TEXT, `variable_map` TEXT, PRIMARY KEY (`campaign_key`))")
            }
        }

        private val MIGRATION_44_45:Migration = object : Migration(44, 45) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE course ADD COLUMN paid_test_id TEXT")
            }
        }
        private val MIGRATION_45_46:Migration = object : Migration(44, 45) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `special_table` (`special_id` INTEGER PRIMARY KEY NOT NULL, `chat_id` TEXT NOT NULL, `created` TEXT, `image_url` TEXT, `instruction_text` TEXT, `main_text` TEXT, `modified` TEXT, `practice_no` INTEGER, `sample_video_url` TEXT, `word_text` TEXT, `sentence_en` TEXT, `word_en` TEXT, `sentence_hi` TEXT, `word_hi` TEXT, `recorded_video` TEXT)")
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
    abstract fun lessonQuestionDao(): LessonQuestionDao
    abstract fun awardMentorModelDao(): AwardMentorModelDao
    abstract fun pendingTaskDao(): PendingTaskDao
    abstract fun practiceEngagementDao(): PracticeEngagementDao
    abstract fun speakingTopicDao(): SpeakingTopicDao
    abstract fun recentSearch(): RecentSearchDao
    abstract fun favoriteCallerDao(): FavoriteCallerDao
    abstract fun courseUsageDao(): CourseUsageDao
    abstract fun commonDao(): CommonDao
    abstract fun voipAnalyticsDao(): VoipAnalyticsDao
    abstract fun groupsAnalyticsDao(): GroupsAnalyticsDao
    abstract fun groupListDao(): GroupListDao
    abstract fun timeTokenDao(): TimeTokenDao
    abstract fun groupChatDao(): GroupChatDao
    abstract fun gameAnalyticsDao(): GameAnalyticsDao
    abstract fun specialDao():SpecialDao
    abstract fun abCampaignDao(): ABTestCampaignDao
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
    fun fromString(value: String?): DOWNLOAD_STATUS {
        return try {
            val matType = object : TypeToken<DOWNLOAD_STATUS>() {}.type
            AppObjectController.gsonMapper.fromJson(value, matType)
        } catch (ex: Exception) {
            ex.printStackTrace()
            DOWNLOAD_STATUS.NOT_START
        }
    }

    @TypeConverter
    fun fromMatType(enumVal: DOWNLOAD_STATUS): String? {
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

class ConvertorForEngagement {
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

class ListConverters {
    @TypeConverter
    fun fromString(value: List<String>?): String {
        val type = object : TypeToken<List<String>>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toString(value: String?): List<String> {
        if (value == null) {
            return Collections.emptyList()
        }
        val type = object : TypeToken<List<String>>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}

class ConvectorForWrongWord {
    @TypeConverter
    fun fromWrongWord(value: List<WrongWord>?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val type = object : TypeToken<List<WrongWord>>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toWrongWord(value: String?): List<WrongWord>? {
        try {
            if (value.isNullOrEmpty()) {
                return null
            }
            val type = object : TypeToken<List<WrongWord>>() {}.type
            return AppObjectController.gsonMapperForLocal.fromJson(value, type)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}

class ConvectorForPhoneticClass {
    @TypeConverter
    fun fromPhonetic(value: List<Phonetic>?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val type = object : TypeToken<List<Phonetic>>() {}.type
        return AppObjectController.gsonMapper.toJson(value, type)
    }

    @TypeConverter
    fun toPhonetic(value: String?): List<Phonetic>? {
        if (value.isNullOrEmpty()) {
            return null
        }
        val type = object : TypeToken<List<Phonetic>>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }
}

class ConverterForLessonQuestionType {
    @TypeConverter
    fun toLessonQuestionType(value: String): LessonQuestionType {
        val type = object : TypeToken<LessonQuestionType>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromLessonQuestionType(enumVal: LessonQuestionType): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}

class ConverterForLessonMaterialType {
    @TypeConverter
    fun toLessonMaterialType(value: String): LessonMaterialType {
        val type = object : TypeToken<LessonMaterialType>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromLessonMaterialType(enumVal: LessonMaterialType): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}

class AwardTypeConverter {
    @TypeConverter
    fun fromString(value: String): AwardTypes {
        val type = object : TypeToken<AwardTypes>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromMatType(enumVal: AwardTypes): String {
        return AppObjectController.gsonMapper.toJson(enumVal)
    }
}

class BigDecimalConverters {
    @TypeConverter
    fun fromString(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }

    @TypeConverter
    fun amountToString(bigDecimal: BigDecimal?): Double? {
        return if (bigDecimal == null) {
            null
        } else {
            bigDecimal.toDouble()
        }
    }
}

class VariableMapConverters {
    @TypeConverter
    fun toVariableMapType(value: String): VariableMap {
        val type = object : TypeToken<VariableMap>() {}.type
        return AppObjectController.gsonMapper.fromJson(value, type)
    }

    @TypeConverter
    fun fromVariableMapType(variableMap: VariableMap): String {
        return AppObjectController.gsonMapper.toJson(variableMap)
    }
}
