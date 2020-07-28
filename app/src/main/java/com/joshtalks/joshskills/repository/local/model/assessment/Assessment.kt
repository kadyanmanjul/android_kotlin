package com.joshtalks.joshskills.repository.local.model.assessment


import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentStatus
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentType
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = "assessments", indices = [
        Index(value = ["remoteId"], unique = true)
    ]
)
@Parcelize
data class Assessment(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "localId")
    @SerializedName("localId")
    val localId: Int = 0,

    @ColumnInfo(name = "remoteId")
    @SerializedName("id")
    var remoteId: Int,

    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    var iconUrl: String?,

    @ColumnInfo(name = "text1")
    @SerializedName("text1")
    var text1: String?,

    @ColumnInfo(name = "text2")
    @SerializedName("text2")
    var text2: String?,

    @ColumnInfo(name = "score_text")
    @SerializedName("score_text")
    var scoreText: String?,

    @ColumnInfo
    @SerializedName("heading")
    val heading: String,

    @ColumnInfo
    @SerializedName("title")
    val title: String?,

    @ColumnInfo
    @SerializedName("image_url")
    val imageUrl: String?,

    @ColumnInfo
    @SerializedName("description")
    val description: String?,

    @TypeConverters(TypeConverterAssessmentType::class)
    @ColumnInfo
    @SerializedName("type")
    val type: AssessmentType,

    @TypeConverters(TypeConverterAssessmentStatus::class)
    @ColumnInfo
    @SerializedName("progress_status")
    var status: AssessmentStatus

) : Parcelable {

    constructor(assessmentResponse: AssessmentResponse) : this(
        remoteId = assessmentResponse.id,
        heading = assessmentResponse.heading,
        title = assessmentResponse.title,
        imageUrl = assessmentResponse.imageUrl,
        description = assessmentResponse.description,
        type = assessmentResponse.type,
        status = assessmentResponse.status,
        iconUrl = assessmentResponse.iconUrl,
        text1 = assessmentResponse.text1,
        text2 = assessmentResponse.text2,
        scoreText = assessmentResponse.scoreText
    )
}
