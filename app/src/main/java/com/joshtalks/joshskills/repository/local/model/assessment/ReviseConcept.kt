package com.joshtalks.joshskills.repository.server.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentMediaType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "assessment_revise_concept", foreignKeys = [
    ForeignKey(
        entity = AssessmentQuestion::class,
        parentColumns = arrayOf("localId"),
        childColumns = arrayOf("questionId"),
        onDelete = ForeignKey.CASCADE
    )], indices = [
    Index(value = ["questionId"]),
    Index(value = ["localId"], unique = true)
]
)
@Parcelize
data class ReviseConcept(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    @SerializedName("localId")
    val localId: Int = 0,

    @ColumnInfo
    @SerializedName("questionId")
    val questionId: Int,

    @ColumnInfo
    @SerializedName("heading")
    val heading: String,

    @ColumnInfo
    @SerializedName("title")
    val title: String,

    @ColumnInfo
    @SerializedName("description")
    val description: String,

    @ColumnInfo
    @SerializedName("media_url")
    val mediaUrl: String,

    @ColumnInfo
    @TypeConverters(TypeConverterAssessmentMediaType::class)
    @SerializedName("media_type")
    val mediaType: AssessmentMediaType,

    @ColumnInfo
    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?

) : Parcelable {
    constructor(reviseConceptResponse: ReviseConceptResponse, questionId: Int) : this(
        questionId = questionId,
        heading = reviseConceptResponse.heading,
        title = reviseConceptResponse.title,
        description = reviseConceptResponse.description,
        mediaUrl = reviseConceptResponse.mediaUrl,
        mediaType = reviseConceptResponse.mediaType,
        videoThumbnailUrl = reviseConceptResponse.videoThumbnailUrl
    )
}
