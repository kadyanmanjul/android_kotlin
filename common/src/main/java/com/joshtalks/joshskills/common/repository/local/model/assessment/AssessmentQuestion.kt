package com.joshtalks.joshskills.common.repository.local.model.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.ListConverters
import com.joshtalks.joshskills.common.repository.local.typeconverter.TypeConverterAssessmentMediaType
import com.joshtalks.joshskills.common.repository.local.typeconverter.TypeConverterChoiceType
import com.joshtalks.joshskills.common.repository.local.typeconverter.TypeConverterQuestionStatus
import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentMediaType
import com.joshtalks.joshskills.common.repository.server.assessment.AssessmentQuestionResponse
import com.joshtalks.joshskills.common.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.common.repository.server.assessment.QuestionStatus
import kotlinx.android.parcel.Parcelize


@Entity(
    tableName = "assessment_questions", foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = arrayOf("remoteId"),
            childColumns = arrayOf("assessmentId"),
            onDelete = ForeignKey.CASCADE
        )], indices = [
        Index(value = ["assessmentId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
@Parcelize
data class AssessmentQuestion(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "localId")
    @SerializedName("localId")
    val localId: Int = 0,

    @ColumnInfo(name = "remoteId")
    @SerializedName("id")
    var remoteId: Int,

    @ColumnInfo
    @SerializedName("assessmentId")
    val assessmentId: Int,

    @ColumnInfo
    @SerializedName("text")
    val text: String?,

    @ColumnInfo
    @SerializedName("sub_text")
    val subText: String?,

    @ColumnInfo
    @SerializedName("sort_order")
    val sortOrder: Int,

    @ColumnInfo
    @SerializedName("media_url")
    val mediaUrl: String,

    @TypeConverters(TypeConverterAssessmentMediaType::class)
    @ColumnInfo
    @SerializedName("media_type")
    val mediaType: AssessmentMediaType,

    @ColumnInfo
    @SerializedName("media_url_2")
    val mediaUrl2: String?,

    @TypeConverters(TypeConverterAssessmentMediaType::class)
    @ColumnInfo
    @SerializedName("media_type_2")
    val mediaType2: AssessmentMediaType = AssessmentMediaType.NONE,

    @ColumnInfo
    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?,

    @TypeConverters(TypeConverterChoiceType::class)
    @ColumnInfo
    @SerializedName("choice_type")
    val choiceType: ChoiceType,

    @ColumnInfo
    @SerializedName("is_attempted")
    var isAttempted: Boolean = false,

    @ColumnInfo
    @SerializedName("is_correct")
    var isCorrect: Boolean = false,

    @ColumnInfo
    @SerializedName("is_new_header")
    var isNewHeader: Boolean = false,

    @TypeConverters(
        ListConverters::class
    )
    @ColumnInfo
    @SerializedName("list_of_answers")
    var listOfAnswers: List<String>?,

    @TypeConverters(TypeConverterQuestionStatus::class)
    @ColumnInfo
    @SerializedName("status")
    var status: QuestionStatus = QuestionStatus.NONE

) : Parcelable {

    constructor(assessmentQuestionResponse: AssessmentQuestionResponse, assessmentId: Int) : this(
        remoteId = assessmentQuestionResponse.id,
        assessmentId = assessmentId,
        text = assessmentQuestionResponse.text,
        subText = assessmentQuestionResponse.subText,
        sortOrder = assessmentQuestionResponse.sortOrder,
        mediaUrl = assessmentQuestionResponse.mediaUrl,
        mediaType = assessmentQuestionResponse.mediaType,
        mediaUrl2 = assessmentQuestionResponse.mediaUrl2,
        mediaType2 = assessmentQuestionResponse.mediaType2,
        videoThumbnailUrl = assessmentQuestionResponse.videoThumbnailUrl,
        choiceType = assessmentQuestionResponse.choiceType,
        isAttempted = assessmentQuestionResponse.isAttempted,
        isNewHeader= assessmentQuestionResponse.isNewHeader,
        status = assessmentQuestionResponse.status,
        listOfAnswers=assessmentQuestionResponse.listOfAnswers
    )

}
