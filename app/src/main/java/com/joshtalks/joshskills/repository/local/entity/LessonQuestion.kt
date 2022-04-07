package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.ConverterForLessonMaterialType
import com.joshtalks.joshskills.repository.local.ConvertorForEngagement
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import java.util.Date
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "lesson_question")
@Parcelize
data class LessonQuestion(

    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id") var id: String = "",

    @ColumnInfo
    @SerializedName("lesson_id")
    var lessonId: Int = 0,

    @ColumnInfo(name = "qText")
    @SerializedName("text")
    var qText: String? = null,

    @ColumnInfo(name = "type")
    @SerializedName("type")
    var type: LessonQuestionType = LessonQuestionType.OTHER,
    // Values can be { Q / PR / P2P / QUIZ / OTHER }

    @TypeConverters(
        ConverterForLessonMaterialType::class
    )
    @ColumnInfo
    @SerializedName("material_type")
    var materialType: LessonMaterialType = LessonMaterialType.OTHER,
    // Values can be { TX / IM / VI / PD / OTHER }

    @ColumnInfo
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false,

    @ColumnInfo
    @SerializedName("title")
    var title: String? = "",

    @ColumnInfo(name = "practice_word")
    @SerializedName("practice_word")
    var practiceWord: String? = "",

    @Ignore
    @SerializedName("audios")
    var audioList: List<AudioType>? = null,

    @Ignore
    @SerializedName("images")
    var imageList: List<ImageType>? = null,

    @Ignore
    @SerializedName("videos")
    var videoList: List<VideoType>? = null,

    @Ignore
    @SerializedName("pdf")
    var pdfList: List<PdfType>? = null,

    @TypeConverters(
        ConvertorForEngagement::class
    )
    @ColumnInfo(name = "practice_engagements")
    @SerializedName("practice_engagements")
    var practiceEngagement: List<PracticeEngagement>? = emptyList(),

    @Ignore
//    @SerializedName("practice_engagements")
    var practiseEngagementV2: List<PracticeEngagementV2>? = emptyList(),

    @Ignore
    @SerializedName("options")
    var optionsList: List<OptionType>? = null,

    @ColumnInfo
    @SerializedName("expected_ans_type")
    var expectedEngageType: EXPECTED_ENGAGE_TYPE? = null,

    @ColumnInfo
    @SerializedName("conversation_question_id")
    var conversation_question_id: Int? = null,

    @ColumnInfo(name = "interval")
    @SerializedName("interval")
    var interval: Int = -1,

    @ColumnInfo(name = "assessment_id")
    @SerializedName("assessment_id")
    var assessmentId: Int? = null,

    @ColumnInfo(name = "topic_id")
    @SerializedName("topic_id")
    var topicId: String? = null,

    @ColumnInfo(name = "chat_type")
    @SerializedName("chat_type")
    var chatType: CHAT_TYPE? = CHAT_TYPE.OTHER,

    @ColumnInfo(name = "status")
    @SerializedName("mentor_que_status")
    var status: QUESTION_STATUS = QUESTION_STATUS.NA,

    @ColumnInfo(name = "vp_sort_order")
    @SerializedName("vp_sort_order")
    var vpSortOrder: Int = -1,

    @ColumnInfo(name = "created")
    @SerializedName("created") var created: Date = Date(),

    @ColumnInfo(name = "modified")
    @SerializedName("modified") var modified: Date = Date(),

    @Ignore
    @Expose
    var isVideoWatchTimeSend: Boolean = false,

    @Ignore
    @IgnoredOnParcel
    var isPlaying: Boolean = false,

    @Ignore
    @IgnoredOnParcel
    var filePath: String? = null,

    @Ignore
    var playProgress: Int = 0,

) : DataBaseClass(), Parcelable {
    init {
        questionId = id
    }
}

enum class LessonQuestionType(val type: String) {
    Q("Q"), // QUESTION
    PR("PR"), // PRACTICE
    QUIZ("QUIZ"), // QUIZ
    P2P("P2P"), // P2P
    CR("CR"), // P2P
    OTHER("OTHER") // Default Value
}

enum class LessonMaterialType(val type: String) {
    TX("TX"), // TEXT
    VI("VI"), // VIDEO
    AU("AU"), // AUDIO
    IM("IM"), // IMAGE
    PD("PD"), // PDF
    OTHER("OTHER") // Default Value
}

@Dao
interface LessonQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionForLesson(question: LessonQuestion)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestionForLessonOnAnyThread(question: LessonQuestion)

    @Update
    suspend fun updateQuestionObject(vararg question: LessonQuestion)

    @Query("UPDATE lesson_question set status = :questionStatus WHERE id=:questionId")
    suspend fun updateQuestionStatus(questionId: String, questionStatus: QUESTION_STATUS)

    @Query("SELECT lessonId FROM lesson_question WHERE id = :questionId")
    fun getLessonIdOfQuestion(questionId: String): Int

    @Query("SELECT * FROM lesson_question WHERE lessonId= :lessonId")
    fun getQuestionsForLesson(lessonId: Int): List<LessonQuestion>

    @Query(value = "SELECT * FROM lesson_question  where id=:questionId")
    suspend fun getLessonQuestionById(questionId: String): LessonQuestion?

    @Transaction
    suspend fun getUpdatedLessonQuestion(questionId: String): LessonQuestion? {
        val question: LessonQuestion? = getLessonQuestionById(questionId)
        if (question != null) {
            when (question.materialType) {
                LessonMaterialType.IM ->
                    question.imageList =
                        AppObjectController.appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.id)
                LessonMaterialType.VI ->
                    question.videoList =
                        AppObjectController.appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.id)
                LessonMaterialType.AU ->
                    question.audioList =
                        AppObjectController.appDatabase.chatDao()
                            .getAudiosOfQuestion(questionId = question.id)
                LessonMaterialType.PD ->
                    question.pdfList =
                        AppObjectController.appDatabase.chatDao()
                            .getPdfOfQuestion(questionId = question.id)
//                LessonMaterialType.OTHER ->
//                    question.imageList =
//                        AppObjectController.appDatabase.chatDao()
//                            .getImagesOfQuestion(questionId = question.id)
//                LessonMaterialType.OTHER ->
//                    question.videoList =
//                        AppObjectController.appDatabase.chatDao()
//                            .getVideosOfQuestion(questionId = question.id)
                else->{
                    question.imageList =
                        AppObjectController.appDatabase.chatDao()
                            .getImagesOfQuestion(questionId = question.id)
                    question.videoList =
                        AppObjectController.appDatabase.chatDao()
                            .getVideosOfQuestion(questionId = question.id)

                }
            }
            if (question.type == LessonQuestionType.PR) {
                question.practiseEngagementV2 =
                    AppObjectController.appDatabase.practiceEngagementDao()
                        .getPractice(question.id)
                question.imageList = AppObjectController.appDatabase.chatDao()
                    .getImagesOfQuestion(questionId = question.id)
            }
        }
        return question
    }

    @Query("UPDATE lesson_question SET downloadStatus = :status , downloadedLocalPath = :path where id=:lessonQuestionId")
    fun updateDownloadStatus(
        lessonQuestionId: String,
        status: DOWNLOAD_STATUS,
        path: String = EMPTY,
    )

    @Query("SELECT COUNT(id) FROM lesson_question WHERE interval= :interval")
    suspend fun getLessonCount(interval: Int): Long
}
