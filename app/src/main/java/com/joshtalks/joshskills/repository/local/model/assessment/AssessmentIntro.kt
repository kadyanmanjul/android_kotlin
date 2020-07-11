package com.joshtalks.joshskills.repository.server.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterChoiceType
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = "assessment_intro", foreignKeys = [
        ForeignKey(
            entity = Assessment::class,
            parentColumns = arrayOf("localId"),
            childColumns = arrayOf("assessmentId"),
            onDelete = ForeignKey.CASCADE
        )], indices = [
        Index(value = ["assessmentId"]),
        Index(value = ["localId"], unique = true),
        Index(value = ["type"], unique = true)
    ]
)
@Parcelize
data class AssessmentIntro(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    @SerializedName("localId")
    val localId: Int = 0,

    @TypeConverters(TypeConverterChoiceType::class)
    @ColumnInfo
    @SerializedName("type")
    val type: ChoiceType,

    @ColumnInfo
    @SerializedName("assessmentId")
    val assessmentId: Int,

    @ColumnInfo
    @SerializedName("title")
    val title: String,

    @ColumnInfo
    @SerializedName("description")
    val description: String,

    @ColumnInfo
    @SerializedName("image_url")
    val imageUrl: String

) : Parcelable {
    constructor(
        assessmentIntroResponse: AssessmentIntroResponse,
        assessmentId: Int
    ) : this(
        assessmentId = assessmentId,
        type = assessmentIntroResponse.type,
        title = assessmentIntroResponse.title,
        description = assessmentIntroResponse.description,
        imageUrl = assessmentIntroResponse.imageUrl
    )
}
