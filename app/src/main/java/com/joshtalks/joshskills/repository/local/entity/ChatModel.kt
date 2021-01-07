package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
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
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.ConvectorForEngagement
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
//import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.repository.local.eventbus.VideoDownloadedBus
import com.joshtalks.joshskills.repository.local.minimalentity.CourseContentEntity
import com.joshtalks.joshskills.util.RandomString
import java.util.Date
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "chat_table", indices = [Index(value = ["chat_id", "conversation_id"])])
data class ChatModel(
    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    @SerializedName("id") var chatId: String = "",

    @ColumnInfo(name = "conversation_id")
    @SerializedName("conversation") var conversationId: String = "",

    @ColumnInfo(name = "created")
    @SerializedName("created") var created: Date,

    @ColumnInfo(name = "is_seen")
    @SerializedName("is_seen") var isSeen: Boolean = false,

    @Ignore
    @SerializedName("question")
    var question: Question? = null,

    @Ignore
    @Expose
    var parentQuestionObject: Question? = null,

    @Embedded
    @SerializedName("sender")
    var sender: Sender? = Sender(),

    @ColumnInfo
    @SerializedName("text") var text: String? = "",

    @ColumnInfo
    @SerializedName("type") var type: BASE_MESSAGE_TYPE? = BASE_MESSAGE_TYPE.TX,

    @ColumnInfo
    @SerializedName("url") var url: String?,

    @ColumnInfo
    var mediaDuration: Long? = 0,


    @ColumnInfo(name = "message_deliver_status")
    var messageDeliverStatus: MESSAGE_DELIVER_STATUS = MESSAGE_DELIVER_STATUS.READ,

    @ColumnInfo(name = "is_sync")
    var isSync: Boolean = true,

    @ColumnInfo(name = "chat_local_id")
    var chatLocalId: String? = RandomString().nextString(),

    @ColumnInfo(name = "is_delete_message")
    var isDeleteMessage: Boolean = false,

    @Expose
    @ColumnInfo(name = "download_progress")
    var progress: Int = 0,

    @ColumnInfo(name = "status")
    @SerializedName("status") var status: MESSAGE_STATUS? = MESSAGE_STATUS.SEEN_BY_SERVER,

    @ColumnInfo
    @SerializedName("question_id") var question_id: Int? = null,

    @ColumnInfo(name = "content_download_date")
    @Expose
    var contentDownloadDate: Date = Date(),

    @ColumnInfo(name = "message_time_in_milliSeconds")
    @SerializedName("createdmilisecond")
    var messageTimeInMilliSeconds: String = EMPTY,

    @ColumnInfo(name = "last_use_time")
    @Expose
    var lastUseTime: Date? = null,

    @ColumnInfo(name = "lesson_id")
    @SerializedName("lesson_id")
    var lessonId: Int = 0,

    @ColumnInfo(name = "award_mentor_id")
    @SerializedName("award_mentor_id") var awardMentorId: Int = 0,

    @Ignore
    @SerializedName("award_mentor") var awardMentorModel: AwardMentorModel? = null,

    @Ignore
    var playProgress: Int = 0,

    @Ignore
    var lessons: LessonModel? = null,

    ) : DataBaseClass(), Parcelable {
    @IgnoredOnParcel
    @Ignore
    var isPlaying: Boolean = false

    @IgnoredOnParcel
    @Ignore
    var filePath: String? = null

    constructor() : this(
        chatId = "",
        conversationId = "",
        created = Date(),
        isSeen = false,
        question = Question(),
        sender = Sender(),
        type = BASE_MESSAGE_TYPE.TX,
        url = "",
        text = "",
        messageDeliverStatus = MESSAGE_DELIVER_STATUS.READ,
        mediaDuration = 0,
        isSync = true,
        chatLocalId = null,
        status = MESSAGE_STATUS.SEEN_BY_SERVER,
        question_id = null,
        contentDownloadDate = Date()
    )


}


@Entity(
    tableName = "question_table"
)
@Parcelize
data class Question(

    @PrimaryKey
    @ColumnInfo(name = "questionId")
    @SerializedName("id") var questionId: String = "",

    @ColumnInfo
    @Expose var chatId: String = "",

    @ColumnInfo
    @SerializedName("course_id") var course_id: Int = 0,

    @ColumnInfo
    @SerializedName("lesson_id") var lesson_id: Int = 0,

    @Ignore
    @SerializedName("lesson") var lesson: LessonModel? = null,

    @Ignore
    @SerializedName("images") var imageList: List<ImageType>? = null,

    @ColumnInfo
    @SerializedName("is_deleted") var isDeleted: Boolean = false,


    @ColumnInfo
    @SerializedName("material_type") var material_type: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.OTHER,

    @Ignore
    @SerializedName("options") var optionsList: List<OptionType>? = null,

    @ColumnInfo
    @SerializedName("parent_id") var parent_id: String? = "",

    @Ignore
    @Expose
    @SerializedName("pdf") var pdfList: List<PdfType>? = null,

    @ColumnInfo(name = "qText")
    @SerializedName("text") var qText: String? = null,

    @ColumnInfo
    @SerializedName("title") var title: String? = "",

    @ColumnInfo(name = "question_type") var questionType: String = "",

    @ColumnInfo(name = "type")
    @SerializedName("type") var type: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.OTHER,


    @Ignore
    @SerializedName("videos") var videoList: List<VideoType>? = emptyList(),

    @Ignore
    @SerializedName("audios") var audioList: List<AudioType>? = null,

    @ColumnInfo(name = "feedback_require")
    @SerializedName("feedback_require") var feedback_require: String? = null,

    @ColumnInfo
    @SerializedName("expected_ans_type") var expectedEngageType: EXPECTED_ENGAGE_TYPE? = null,

    @ColumnInfo(name = "practice_no")
    @SerializedName("practice_no") var practiceNo: Int? = null,

    @ColumnInfo(name = "need_feedback")
    @Expose var needFeedback: Boolean? = null,

    @ColumnInfo(name = "upload_feedback_status")
    @Expose var uploadFeedbackStatus: Boolean = false,

    @ColumnInfo(name = "interval")
    @SerializedName("interval") var interval: Int = -1,

    @ColumnInfo(name = "assessment_id")
    @SerializedName("assessment_id") var assessmentId: Int? = null,

    @ColumnInfo(name = "conversation_practice_id")
    @SerializedName("conversation_practice_id") var conversationPracticeId: String? = null,

    @ColumnInfo(name = "temp_type")
    @Expose var tempType: BASE_MESSAGE_TYPE? = type,

    @ColumnInfo(name = "practice_word")
    @SerializedName("practice_word") var practiceWord: String? = "",

    @ColumnInfo(name = "chat_type")
    @SerializedName("chat_type")
    var chatType: CHAT_TYPE? = CHAT_TYPE.OTHER,

    @ColumnInfo(name = "status")
    @SerializedName("mentor_que_status")
    var status: QUESTION_STATUS = QUESTION_STATUS.NA,

    @ColumnInfo(name = "lesson_status")
    @SerializedName("mentor_lesson_status")
    var lessonStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @Ignore
    @Expose var vAssessmentCount: Int = -1,
    @Ignore
    @Expose var isVideoWatchTimeSend: Boolean = false,

    @ColumnInfo(name = "certificate_exam_id")
    @SerializedName("certificateexam_id") var certificateExamId: Int? = null,

    @ColumnInfo(name = "topic_id")
    @SerializedName("topic_id") var topicId: String? = null,

    @ColumnInfo(name = "vp_sort_order")
    @SerializedName("vp_sort_order") var vpSortOrder: Int = -1,

    @Embedded(prefix = "cexam_")
    @Expose
    var cexamDetail: CertificationExamDetailModel? = null,

    @Expose
    @Ignore
    var vocabOrder: Int? = null,

    @TypeConverters(
        ConvectorForEngagement::class
    )
    @ColumnInfo(name = "practice_engagements")
    // @SerializedName("practice_engagements")
    var practiceEngagement: List<PracticeEngagement>? = emptyList(),

    @Ignore
    @SerializedName("practice_engagements")
    var practiseEngagementV2: List<PracticeEngagementV2>? = emptyList(),

) : Parcelable


data class User(
    @SerializedName("first_name") var first_name: String = "",

    @SerializedName("id") var id: String = "",

    @SerializedName("last_name") var last_name: String = "",

    @SerializedName("photo_url") var photo_url: String? = "",

    @SerializedName("user_type") var user_type: String = ""
) : java.io.Serializable


@Entity(tableName = "VideoTable")
@Parcelize
data class VideoType(
    @ColumnInfo
    @SerializedName("video_url") var video_url: String? = "",

    @PrimaryKey
    @ColumnInfo
    @SerializedName("id") var id: String = "",

    @ColumnInfo
    @SerializedName("thumbnail_url") var video_image_url: String = "",

    @ColumnInfo
    @SerializedName("duration") var duration: Int = 0,

    @ColumnInfo
    @SerializedName("video_height") var video_height: Int = 0,

    @ColumnInfo
    @SerializedName("video_width") var video_width: Int = 0,

    @ColumnInfo
    @SerializedName("thumbnail_height") var thumbnail_height: Int = 0,

    @ColumnInfo
    @SerializedName("thumbnail_width") var thumbnail_width: Int = 0,

    @ColumnInfo
    @SerializedName("bit_rate") var bit_rate: Int = 0,

    @ColumnInfo
    @SerializedName("is_deleted") var is_deleted: Boolean = false,

    @ColumnInfo
    @SerializedName("interval") var interval: Int = -1

) : DataBaseClass(), Parcelable


@Entity(tableName = "AudioTable")
@Parcelize
data class AudioType(
    @ColumnInfo
    @SerializedName("audio_url") var audio_url: String = "",

    @PrimaryKey
    @ColumnInfo(name = "audioId")
    @SerializedName("id") var id: String = "",

    @ColumnInfo
    @SerializedName("duration") var duration: Int = 0,

    @ColumnInfo
    @SerializedName("bit_rate") var bit_rate: Int = 0,

    @ColumnInfo
    @SerializedName("is_deleted") var is_deleted: Boolean = false

) : DataBaseClass(), Parcelable


@Entity(tableName = "OptionTable")
@Parcelize
data class OptionType(

    @PrimaryKey
    @ColumnInfo
    @SerializedName("id") var id: String = "",


    @ColumnInfo
    @SerializedName("type") var type: String = "",

    @ColumnInfo
    @SerializedName("text") var text: String = "",


    @ColumnInfo
    @SerializedName("order") var order: Int = 0

) : DataBaseClass(), Parcelable


@Entity(tableName = "PdfTable")
@Parcelize
data class PdfType(

    @PrimaryKey
    @ColumnInfo
    @SerializedName("id") var id: String = "",


    @ColumnInfo
    @SerializedName("url") var url: String = "",

    @ColumnInfo
    @SerializedName("is_deleted") var is_deleted: Boolean = false,

    @ColumnInfo(name = "total_view")
    var totalView: Int = 0,

    @ColumnInfo(name = "thumbnail")
    var thumbnail: String? = "",

    @ColumnInfo(name = "size")
    var size: String = "",

    @ColumnInfo(name = "pages")
    var pages: String? = ""

) : DataBaseClass(), Parcelable


@Entity(tableName = "ImageTable")
@Parcelize
data class ImageType(

    @PrimaryKey
    @ColumnInfo
    @SerializedName("id") var id: String = "",

    @ColumnInfo
    @SerializedName("image_url") var imageUrl: String = "",

    @ColumnInfo
    @SerializedName("is_deleted") var is_deleted: Boolean = false,

    @ColumnInfo
    @SerializedName("height") var height: Int = 0,

    @ColumnInfo
    @SerializedName("width") var width: Int = 0

) : DataBaseClass(), Parcelable


data class Sender(
    @SerializedName("id") var id: String = "",
    @SerializedName("user") var user: User? = User(),
    @SerializedName("user_type") var user_type: String = ""
) : java.io.Serializable


data class PracticeEngagement(
    @SerializedName("answer_url") val answerUrl: String?,
    @SerializedName("id") val id: Int?,
    @SerializedName("text") val text: String?,
    @SerializedName("duration") var duration: Int?,
    @SerializedName("feedback") var practiceFeedback: PracticeFeedback?,
    @SerializedName("practice_date") val practiceDate: String?,
    @SerializedName("transcript_id") val transcriptId: String?,
    @SerializedName("points_list") val pointsList: List<String>?,
    @Expose var localPath: String? = null

) : java.io.Serializable {
    constructor() : this(
        answerUrl = null,
        id = null,
        text = null,
        duration = null,
        practiceFeedback = null,
        practiceDate = null,
        transcriptId = null,
        pointsList = emptyList()
    )
}

data class PracticeFeedback(
    @SerializedName("title") val title: String?,
    @SerializedName("grade") val grade: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("gif_url") var gifUrl: String?,
) : java.io.Serializable {
    constructor() : this(
        title = null,
        grade = null,
        text = null,
        gifUrl = null
    )
}

data class PracticeFeedback2(
    @SerializedName("status") val status: String?,
    @SerializedName("engagement") val engagementId: Int?,
    @SerializedName("text") val text: String?,
    @SerializedName("grade") val grade: String?,
    @SerializedName("score") var score: String?,
) : java.io.Serializable {
    constructor() : this(
        status = null,
        engagementId = null,
        text = null,
        score = null,
        grade = null
    )
}


open class DataBaseClass(
    @ColumnInfo
    @Expose
    var questionId: String = "",

    @ColumnInfo
    @Expose
    var downloadStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.DOWNLOADED,

    @ColumnInfo
    @Expose
    var downloadedLocalPath: String? = "",

    @ColumnInfo
    @Expose
    var lastDownloadStartTime: Long = 0,

    @ColumnInfo
    @Expose
    var thumbnailUrl: String? = "",

    @Ignore
    @Expose
    var isSelected: Boolean = false,

    @Ignore
    @Expose
    var disable: Boolean = false
)

@Dao
interface ChatDao {

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND is_delete_message=0 ORDER BY created ASC,question_id ASC ")
    suspend fun getLastChats(conversationId: String): List<ChatModel>

    @Query(value = "SELECT * FROM chat_table where lesson_id= :lessonId AND is_delete_message=0 ORDER BY created ASC,question_id ASC ")
    suspend fun getChatsForLessonId(lessonId: Int): List<ChatModel>

    @Query(value = "SELECT * FROM chat_table  where chat_id=:chatId")
    suspend fun getChatObject(chatId: String): ChatModel

    @Query(value = "SELECT * FROM chat_table  where chat_id=:chatId")
    suspend fun getNullableChatObject(chatId: String): ChatModel?


    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND created > :compareTime AND is_delete_message=0  ORDER BY created ASC,question_id ASC")
    suspend fun getRecentChatAfterTime(conversationId: String, compareTime: Date?): List<ChatModel>


    @Query(value = "SELECT COUNT(chat_id) FROM chat_table where conversation_id= :conversationId ")
    suspend fun getTotalCountOfRows(conversationId: String): Long


    @Query("UPDATE chat_table SET message_deliver_status = :messageDeliverStatus where created <= :compareTime ")
    fun updateSeenMessages(
        messageDeliverStatus: MESSAGE_DELIVER_STATUS = MESSAGE_DELIVER_STATUS.READ,
        compareTime: Date
    )

    @Query("UPDATE chat_table SET is_sync =1 where chat_id <= :id ")
    suspend fun forceFullySync(id: String)

    @Query(value = "SELECT * FROM chat_table where  is_sync= 0")
    suspend fun getUnSyncMessage(): List<ChatModel>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAMessage(chat: ChatModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateChatMessage(chat: ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateChatMessageOnAnyThread(chat: ChatModel)

    @Delete
    suspend fun deleteChatMessage(chat: ChatModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageList(chatModelList: List<ChatModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatQuestion(question: Question)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatQuestions(question: List<Question>)

    @Query("SELECT * FROM question_table WHERE chatId= :chatId")
    suspend fun getQuestion(chatId: String): Question?

    @Query("SELECT * FROM question_table WHERE questionId= :questionId")
    suspend fun getQuestionByQuestionId(questionId: String): Question?

    @Query("SELECT * FROM question_table WHERE lesson_id= :lessonId")
    fun getQuestionsForLesson(lessonId: String): LiveData<List<Question>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioMessageList(audioList: List<AudioType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoMessageList(audioList: List<VideoType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfMessageList(pdfList: List<PdfType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptionTypeMessageList(audioList: List<OptionType>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageTypeMessageList(audioList: List<ImageType>)

    @Query("SELECT * FROM ImageTable WHERE questionId= :questionId")
    suspend fun getImagesOfQuestion(questionId: String): List<ImageType>

    @Query("SELECT * FROM VideoTable WHERE questionId= :questionId")
    suspend fun getVideosOfQuestion(questionId: String): List<VideoType>

    @Query("SELECT * FROM AudioTable WHERE questionId= :questionId")
    suspend fun getAudiosOfQuestion(questionId: String): List<AudioType>

    @Query("SELECT * FROM PdfTable WHERE questionId= :questionId")
    suspend fun getPdfOfQuestion(questionId: String): List<PdfType>

    @Update
    fun updateAudioObject(vararg audioList: AudioType)

    @Update
    fun updateImageObject(vararg imageObj: ImageType)

    @Update
    fun updateVideoObject(vararg videoObj: VideoType)

    @Update
    fun updatePdfObject(vararg pdfObj: PdfType)

    @Transaction
    suspend fun getUpdatedChatObject(chat: ChatModel): ChatModel {
        val chatModel: ChatModel = getChatObject(chatId = chat.chatId)
        if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(chat.chatId)
            if (question != null) {
                when (question.material_type) {
                    BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI -> question.videoList =
                        getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU -> question.audioList =
                        getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        getPdfOfQuestion(questionId = question.questionId)
                }
                chatModel.question = question
            }
        }
        return chatModel
    }

    @Transaction
    suspend fun getUpdatedChatObjectViaId(id: String): ChatModel {
        val chatModel: ChatModel = getChatObject(chatId = id)
        if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(id)
            if (question != null) {
                when (question.material_type) {
                    BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI -> question.videoList =
                        getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU -> question.audioList =
                        getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        getPdfOfQuestion(questionId = question.questionId)
                }
                if (question.type == BASE_MESSAGE_TYPE.PR) {
                    question.practiseEngagementV2 =
                        AppObjectController.appDatabase.practiceEngagementDao()
                            .getPractice(question.questionId)
                    question.imageList = AppObjectController.appDatabase.chatDao()
                        .getImagesOfQuestion(questionId = question.questionId)
                }
                if (question.lesson != null) {
                    chatModel.lessons = question.lesson
                }

                chatModel.question = question
                val lessonModel =
                    AppObjectController.appDatabase.lessonDao().getLesson(question.lesson_id)
                chatModel.lessons = lessonModel
                chatModel.question?.lesson = lessonModel
            }
        }
        return chatModel
    }


    @Transaction
    suspend fun getUpdatedQuestionObjectViaId(id: String): ChatModel {
        val chatModel: ChatModel = getChatObject(chatId = id)
        if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(id)
            if (question != null) {
                when (question.material_type) {
                    BASE_MESSAGE_TYPE.IM ->
                        question.imageList =
                            getImagesOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.VI -> question.videoList =
                        getVideosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.AU -> question.audioList =
                        getAudiosOfQuestion(questionId = question.questionId)
                    BASE_MESSAGE_TYPE.PD -> question.pdfList =
                        getPdfOfQuestion(questionId = question.questionId)
                }
                chatModel.question = question
            }
        }
        return chatModel
    }

    @Transaction
    suspend fun updateDownloadVideoStatus(obj: ChatModel, downloadStatus: DOWNLOAD_STATUS) {
        val chatModel: ChatModel = getChatObject(chatId = obj.chatId)
        chatModel.downloadStatus = downloadStatus
        updateChatMessage(chatModel)
        obj.question?.videoList?.get(0)?.let {
            it.downloadStatus = downloadStatus
            updateVideoObject(it)
        }
        if (downloadStatus == DOWNLOAD_STATUS.FAILED || downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
            RxBus2.publish(VideoDownloadedBus(obj))
        }
    }

    @Query("UPDATE chat_table SET downloadStatus = :status where downloadStatus == :whereStatus")
    suspend fun updateDownloadVideoStatusFailed(
        status: DOWNLOAD_STATUS = DOWNLOAD_STATUS.NOT_START,
        whereStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.DOWNLOADING
    )

    @Query(value = "UPDATE PdfTable SET total_view = :total_view where id= :id ")
    suspend fun updateTotalViewForPdf(id: String, total_view: Int)

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId ORDER BY ID DESC LIMIT 1")
    suspend fun getLastOneChat(conversationId: String): ChatModel?

    suspend fun getMaxIntervalForVideo(conversationId: String): Int {

        val chatModel: ChatModel? = getLastQuestionInterval(conversationId)
        if (chatModel?.type == BASE_MESSAGE_TYPE.Q) {
            val question: Question? = getQuestion(chatModel.chatId)
            if (question != null) {
                return question.interval.plus(1)
            }
        }
        return 0
    }

    @Query("SELECT * FROM question_table WHERE course_id= :course_id AND interval=:interval")
    suspend fun getQuestionForNextInterval(course_id: String, interval: Int): Question?

    @Query(value = "SELECT * FROM chat_table where conversation_id= :conversationId AND type= :type ")
    suspend fun getUnlockChatModel(
        conversationId: String,
        type: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.UNLOCK
    ): List<ChatModel?>?

    @Query(value = "SELECT * FROM chat_table where question_id IS NOT NULL AND conversation_id= :conversationId ORDER BY created DESC LIMIT 1; ")
    suspend fun getLastQuestionInterval(conversationId: String): ChatModel?

    @Query(value = "DELETE FROM chat_table where conversation_id= :conversationId AND type=:type")
    suspend fun deleteSpecificTypeChatModel(conversationId: String, type: BASE_MESSAGE_TYPE)

    @Query(value = "UPDATE chat_table  SET is_seen = 1 where conversation_id= :conversationId")
    suspend fun readAllChatBYUser(conversationId: String)

    @Query("UPDATE chat_table SET is_delete_message =1 WHERE chat_id IN (:ids)")
    suspend fun changeStatusForDeleteMessage(ids: List<String>)

    @Query(value = "SELECT * FROM chat_table where is_delete_message=1 ")
    suspend fun getUnsyncDeletesMessage(): List<ChatModel>

    @Query("DELETE FROM chat_table where  chat_id IN (:ids)")
    suspend fun deleteUserMessages(ids: List<String>)

    @Query("UPDATE chat_table SET download_progress = :progress where id= :conversationId ")
    suspend fun videoProgressUpdate(conversationId: String, progress: Int)

    @Query("SELECT * FROM chat_table where conversation_id= :conversationId  ORDER BY created ASC LIMIT 1;")
    suspend fun getLastRecord(conversationId: String): ChatModel

    @Query("UPDATE chat_table SET status = :status WHERE chat_id IN (:ids)")
    suspend fun updateMessageStatus(status: MESSAGE_STATUS, ids: List<String>)

    @Query(value = "SELECT chat_id FROM chat_table where status=:status")
    suspend fun getSeenByUserMessages(status: MESSAGE_STATUS = MESSAGE_STATUS.SEEN_BY_USER): List<String>

    @Update
    suspend fun updateQuestionObject(vararg question: Question)

    @Query("UPDATE question_table set status = :questionStatus WHERE questionId=:questionId")
    suspend fun updateQuestionStatus(questionId: String, questionStatus: QUESTION_STATUS)

    @Query("UPDATE question_table set status = :questionStatus AND lesson_status = :lessonStatus WHERE questionId=:questionId")
    suspend fun updateQuestionAndLessonStatus(
        questionId: String,
        questionStatus: QUESTION_STATUS,
        lessonStatus: LESSON_STATUS
    )

    @Query("SELECT lesson_id FROM question_table WHERE questionId = :questionId")
    fun getLessonIdOfQuestion(questionId: String): Int

    @Query("UPDATE question_table SET practice_engagements = :practiseEngagement  WHERE questionId= :questionId")
    suspend fun updatePractiseObject(
        questionId: String,
        practiseEngagement: List<PracticeEngagement>
    )

    @Query("SELECT practice_engagements FROM question_table  WHERE questionId= :questionId")
    suspend fun getPractiseObject(
        questionId: String
    ): String?

    @Query(value = "SELECT message_time_in_milliSeconds FROM chat_table where question_id IS NOT NULL AND conversation_id= :conversationId ORDER BY created DESC LIMIT 1; ")
    suspend fun getLastChatDate(conversationId: String): String?

    @Query(value = "SELECT * FROM (SELECT *,qt.type AS 'question_type' FROM chat_table ct LEFT JOIN question_table qt ON ct.chat_id = qt.chatId where qt.type= :typeO AND  title IS NOT NULL ) inbox  where type= :typeO AND conversation_id= :conversationId  ORDER BY created ASC;")
    suspend fun getRegisterCourseMinimal22(
        conversationId: String,
        typeO: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.Q
    ): List<CourseContentEntity>

    @Query("SELECT * FROM  question_table  WHERE questionId= :questionId")
    suspend fun getQuestionOnId(questionId: String): Question?

    @Query("SELECT * FROM  question_table  WHERE questionId= :questionId")
    fun getQuestionOnIdV2(questionId: String): Question?


    @Transaction
    suspend fun getPractiseFromQuestionId(id: String): ChatModel? {
        val question: Question? = getQuestionOnId(id)
        return if (question == null) null
        else {
            getUpdatedChatObjectViaId(question.chatId)
        }
    }

    @Transaction
    suspend fun getChatFromQuestionId(chatId: String): ChatModel? {
        return getUpdatedChatObjectViaId(chatId)
    }

    @Query("SELECT * FROM  PdfTable  WHERE id= :pdfId")
    suspend fun getPdfById(pdfId: String): PdfType

    @Query("UPDATE question_table SET need_feedback = :status WHERE questionId= :questionId")
    suspend fun updateFeedbackStatus(questionId: String, status: Boolean?)

    @Query("SELECT need_feedback from question_table  WHERE questionId= :questionId AND upload_feedback_status=0;")
    suspend fun getFeedbackStatusOfQuestion(questionId: String): Boolean?

    @Query("UPDATE question_table SET upload_feedback_status = 1 WHERE questionId= :questionId")
    suspend fun userSubmitFeedbackStatusUpdate(questionId: String)

    @Query("UPDATE chat_table SET last_use_time = :date where chat_id=:chatId ")
    fun lastUsedBy(chatId: String, date: Date = Date())

    @Query(value = "SELECT * FROM chat_table where last_use_time ORDER BY last_use_time ASC")
    suspend fun getAllRecentDownloadMedia(): List<ChatModel>


    @Query("SELECT * FROM  question_table  WHERE certificate_exam_id= :certificateExamId")
    suspend fun getQuestionUsingCExamId(certificateExamId: Int): Question?


    @Transaction
    suspend fun insertCertificateExamDetail(
        certificateExamId: Int,
        obj: CertificationExamDetailModel
    ) {
        val question: Question? = getQuestionUsingCExamId(certificateExamId)
        if (question != null) {
            question.cexamDetail = obj
            updateQuestionObject(question)
        }
    }
}

enum class OPTION_TYPE(val type: String) {
    OPTION("O"), POLL("P")
}

enum class BASE_MESSAGE_TYPE(val type: String) {
    A("A"), TX("TX"), VI("VI"), AU("AU"), IM("IM"), Q("Q"), PD("PD"), PR("PR"), AR("AR"),
    CP("CP"), QUIZ("QUIZ"), TEST("TEST"), OTHER("OTHER"), UNLOCK("UN"), P2P("P2P"),
    LESSON("LESSON"), CE("CE"), BEST_PERFORMER("BEST_PERFORMER")
}


enum class EXPECTED_ENGAGE_TYPE(val type: String) {
    TX("TX"), VI("VI"), AU("AU"), IM("IM"), DX("DX")

}


enum class MESSAGE_DELIVER_STATUS(val type: Int) {
    SENT(0), SENT_RECEIVED(1), READ(2)
}


enum class DOWNLOAD_STATUS {
    DOWNLOADED, DOWNLOADING, FAILED, NOT_START, UPLOADING, UPLOADED, STARTED
}

enum class MESSAGE_STATUS(val type: String) {
    SEEN_BY_USER("seen_by_user"), DELIVERED("delivered"), SEEN_BY_SERVER("seen")
}

enum class QUESTION_STATUS(val type: String) {
    NA("NA"), AT("AT"), IP("IP")
}

enum class LESSON_STATUS(val type: String) {
    NO("NO"), AT("AT"), CO("CO")
}

enum class CHAT_TYPE(val type: String) {
    GR("GR"), VP("VP"), RP("RP"), OTHER("OTHER"), SOTD("SOTD"), SOTW("SOTW"), SOTM("SOTM"), SOTY("SOTY")
}
