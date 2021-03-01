package com.joshtalks.joshskills.repository.local.entity

//import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.ConvectorForEngagement
import com.joshtalks.joshskills.repository.local.entity.practise.PracticeEngagementV2
import com.joshtalks.joshskills.util.RandomString
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
@Entity(
    tableName = "chat_table",
    indices = [Index(value = ["chat_id", "conversation_id", "created"])]
)
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
    var duration: Int = 0,

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

    constructor(type: BASE_MESSAGE_TYPE, text: String = EMPTY) : this(
        type = type,
        text = text,
        chatId = "",
        conversationId = "",
        created = Date(),
        isSeen = false,
        question = Question(),
        sender = Sender(),
        url = "",
        messageDeliverStatus = MESSAGE_DELIVER_STATUS.READ,
        duration = 0,
        isSync = true,
        chatLocalId = null,
        status = MESSAGE_STATUS.SEEN_BY_SERVER,
        question_id = null,
        contentDownloadDate = Date()
    )


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
        duration = 0,
        isSync = true,
        chatLocalId = null,
        status = MESSAGE_STATUS.SEEN_BY_SERVER,
        question_id = null,
        contentDownloadDate = Date()
    )

    override fun hashCode(): Int {
        return chatId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other?.javaClass != javaClass) return false
        if (chatId == (other as ChatModel).chatId) {
            return true
        }
        return false
    }
}


@Entity(
    tableName = "question_table", foreignKeys = [
        ForeignKey(
            entity = ChatModel::class,
            parentColumns = arrayOf("chat_id"),
            childColumns = arrayOf("chatId"),
            onDelete = ForeignKey.CASCADE
        )], indices = [
        Index(value = ["questionId"], unique = true)
    ]
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
    @SerializedName("practice_engagements")
    var practiceEngagement: List<PracticeEngagement>? = emptyList(),

    @Ignore
//    @SerializedName("practice_engagements")
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

enum class OPTION_TYPE(val type: String) {
    OPTION("O"), POLL("P")
}

enum class BASE_MESSAGE_TYPE(val type: String) {
    A("A"), TX("TX"), VI("VI"), AU("AU"), IM("IM"), Q("Q"), PD("PD"), PR("PR"), AR("AR"),
    CP("CP"), QUIZ("QUIZ"), TEST("TEST"), OTHER("OTHER"), UNLOCK("UN"), P2P("P2P"),
    LESSON("LESSON"), CE("CE"), BEST_PERFORMER("BEST_PERFORMER"), NEW_CLASS("NEW_CLASS"),
}


enum class EXPECTED_ENGAGE_TYPE(val type: String) {
    TX("TX"), VI("VI"), AU("AU"), IM("IM"), DX("DX")

}


enum class MESSAGE_DELIVER_STATUS(val type: Int) {
    SENT(0), SENT_RECEIVED(1), READ(2)
}


enum class DOWNLOAD_STATUS {
    DOWNLOADED, DOWNLOADING, FAILED, NOT_START, UPLOADING, UPLOADED, STARTED, REQUEST_DOWNLOADING
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
