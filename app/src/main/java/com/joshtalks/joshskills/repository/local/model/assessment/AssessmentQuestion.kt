package com.joshtalks.joshskills.repository.local.model.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentMediaType
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterChoiceType
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterQuestionStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentMediaType
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionResponse
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
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
    val text: String,

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
    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?,

    @TypeConverters(TypeConverterChoiceType::class)
    @ColumnInfo
    @SerializedName("choice_type")
    val choiceType: ChoiceType,

    @ColumnInfo
    @SerializedName("is_attempted")
    val isAttempted: Boolean = false,

    @TypeConverters(TypeConverterQuestionStatus::class)
    @ColumnInfo
    @SerializedName("status")
    val status: QuestionStatus = QuestionStatus.NONE

) : Parcelable {

    constructor(assessmentQuestionResponse: AssessmentQuestionResponse, assessmentId: Int) : this(
        remoteId = assessmentQuestionResponse.id,
        assessmentId = assessmentId,
        text = assessmentQuestionResponse.text,
        sortOrder = assessmentQuestionResponse.sortOrder,
        mediaUrl = assessmentQuestionResponse.mediaUrl,
        mediaType = assessmentQuestionResponse.mediaType,
        videoThumbnailUrl = assessmentQuestionResponse.videoThumbnailUrl,
        choiceType = assessmentQuestionResponse.choiceType,
        isAttempted = assessmentQuestionResponse.isAttempted,
        status = assessmentQuestionResponse.status
    )

}
