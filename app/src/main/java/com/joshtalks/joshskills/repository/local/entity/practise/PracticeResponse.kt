package com.joshtalks.joshskills.repository.local.entity.practise

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.ConvectorForPhoneticClass
import com.joshtalks.joshskills.repository.local.ConvectorForWrongWord
import com.joshtalks.joshskills.repository.local.ListConverters
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.Question
import kotlinx.android.parcel.Parcelize
import java.util.*


@Entity(
    tableName = "practise_engagement_table",
    indices = [Index(value = ["practiseId", "question"])],
    foreignKeys = [ForeignKey(
        entity = Question::class,
        parentColumns = arrayOf("questionId"),
        childColumns = arrayOf("questionForId"),
        onDelete = ForeignKey.CASCADE
    )]
)
@Parcelize
data class PracticeEngagementV2(
    @ColumnInfo()
    var questionForId: String? = EMPTY,

    @PrimaryKey()
    @SerializedName("id")
    var practiseId: Int = 0,

    @ColumnInfo()
    @SerializedName("error")
    var isErrorFeedback: Boolean = true,

    @ColumnInfo()
    @SerializedName("question")
    var question: Int = 0,

    @ColumnInfo()
    @SerializedName("answer_url")
    var answerUrl: String = EMPTY,

    @ColumnInfo()
    @SerializedName("duration")
    var duration: Int = 0,

    @ColumnInfo()
    @SerializedName("practice_date")
    var practiceDate: String = EMPTY,

    @ColumnInfo()
    @SerializedName("feedback_require")
    var feedbackRequire: String? = EMPTY,

    @ColumnInfo()
    @SerializedName("text")
    var text: String? = EMPTY,

    @ColumnInfo()
    @Expose var localPath: String? = EMPTY,

    @ColumnInfo()
    @SerializedName("transcript_id")
    var transcriptId: String? = EMPTY,

    @TypeConverters(
        ListConverters::class
    )
    @ColumnInfo
    @SerializedName("points_list")
    var pointsList: List<String> = arrayListOf(),

    @Embedded(prefix = "feedback_")
    @SerializedName("feedback")
    var practiseFeedback: PractiseFeedback? = null,

    @Expose
    @Ignore
    var practiseType: PractiseType = PractiseType.SUBMITTED,

    @ColumnInfo
    @Expose
    var uploadStatus: DOWNLOAD_STATUS = DOWNLOAD_STATUS.UPLOADED,


    ) : Parcelable {
    constructor() : this(
        questionForId = EMPTY,
        practiseId = 0,
        question = 0,
        answerUrl = EMPTY,
        duration = 0,
        practiceDate = EMPTY,
        feedbackRequire = null,
        text = null,
        localPath = null,
        transcriptId = null,
        pointsList = emptyList(),
        practiseType = PractiseType.SUBMITTED,
        uploadStatus = DOWNLOAD_STATUS.UPLOADED,
        isErrorFeedback = true
    )
}

@Parcelize
data class PractiseFeedback(
    @SerializedName("id")
    val feedbackId: Int? = 0,
    @SerializedName("feedback_title")
    val feedbackTitle: String? = EMPTY,
    @SerializedName("feedback_text")
    val feedbackText: String? = EMPTY,
    @SerializedName("student_audio_url")
    val studentAudioUrl: String? = EMPTY,
    @SerializedName("teacher_audio_url")
    val teacherAudioUrl: String? = EMPTY,

    @Embedded(prefix = "pro_")
    @SerializedName("pronunciation")
    val pronunciation: Pronunciation? = null,

    @Embedded(prefix = "rec_")
    @SerializedName("recommendation")
    val recommendation: Recommendation? = null,

    @Embedded(prefix = "spd_")
    @SerializedName("speed")
    val speed: Speed? = null,


    @TypeConverters(
        ConvectorForWrongWord::class
    )
    @ColumnInfo(name = "wrong_word_list")

    @SerializedName("wrong_word_list")
    val pointsList: List<WrongWord>? = null,

    @SerializedName("created")
    val created: Date = Date(),

    @ColumnInfo
    @SerializedName("error")
    val error: Boolean = false

) : Parcelable {
    constructor() : this(
        feedbackId = 0,
        created = Date(),
        feedbackTitle = EMPTY,
        feedbackText = EMPTY,
        studentAudioUrl = EMPTY,
        teacherAudioUrl = EMPTY,
        pronunciation = Pronunciation(),
        recommendation = Recommendation(),
        speed = Speed(),
        pointsList = emptyList(),
        error = false
    )
}

@Parcelize
data class Pronunciation(
    @SerializedName("text")
    val text: String = EMPTY,
    @SerializedName("description")
    val description: String = EMPTY
) : Parcelable {
    constructor() : this(
        text = EMPTY,
        description = EMPTY
    )
}

@Parcelize
data class Recommendation(
    @SerializedName("text")
    val text: String = EMPTY
) : Parcelable {
    constructor() : this(
        text = EMPTY,
    )
}

@Parcelize
data class Speed(
    @SerializedName("text")
    val text: String = EMPTY,
    @SerializedName("description")
    val description: String = EMPTY
) : Parcelable {
    constructor() : this(
        text = EMPTY,
        description = EMPTY
    )
}

@Parcelize
data class WrongWord(
    @SerializedName("word")
    val word: String = EMPTY,

    @TypeConverters(
        ConvectorForPhoneticClass::class
    )
    @SerializedName("phones")
    val phones: List<Phonetic>? = emptyList(),

    @SerializedName("student_start_time")
    val studentStartTime: Long = 0,
    @SerializedName("student_end_time")
    val studentEndTime: Long = 0,
    @SerializedName("teacher_start_time")
    val teacherStartTime: Long = 0,
    @SerializedName("teacher_end_time")
    val teacherEndTime: Long = 0,

    ) : Parcelable {
    constructor() : this(
        studentStartTime = 0,
        studentEndTime = 0,
        teacherStartTime = 0,
        teacherEndTime = 0,
        word = EMPTY
    )
}

@Parcelize
data class Phonetic(
    @SerializedName("phone")
    val phone: String = EMPTY,
    @SerializedName("quality")
    val quality: String = EMPTY
) : Parcelable {
    constructor() : this(
        phone = EMPTY,
        quality = EMPTY
    )
}

enum class PractiseType {
    SUBMITTED, NOT_SUBMITTED
}


@Dao
interface PracticeEngagementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPractise(practiceEngagementV2: PracticeEngagementV2): Long

    @Query(value = "SELECT * FROM practise_engagement_table where  questionForId= :questionId")
    suspend fun getPractice(questionId: String): List<PracticeEngagementV2>?

    @Query(value = "DELETE FROM practise_engagement_table where   questionForId= :questionId AND uploadStatus=:type")
    suspend fun deleteTempPractise(
        questionId: String,
        type: DOWNLOAD_STATUS = DOWNLOAD_STATUS.UPLOADING
    )

    @Transaction
    suspend fun insertPractiseAfterUploaded(practiceEngagementV2: PracticeEngagementV2) {
        deleteTempPractise(practiceEngagementV2.questionForId!!)
        insertPractise(practiceEngagementV2)
    }
}

