package com.joshtalks.joshskills.repository.server.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterAssessmentType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "assessment_intro")
@Parcelize
data class AssessmentIntro(

    @TypeConverters(TypeConverterAssessmentType::class)
    @PrimaryKey
    @ColumnInfo
    @SerializedName("type")
    val type: AssessmentType,

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

) : Parcelable
