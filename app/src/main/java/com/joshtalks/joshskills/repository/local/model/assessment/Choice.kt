package com.joshtalks.joshskills.repository.local.model.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.ConvertersForDownloadStatus
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.type_converter.TypeConverterChoiceType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceColumn
import com.joshtalks.joshskills.repository.server.assessment.ChoiceResponse
import kotlinx.android.parcel.Parcelize

@Entity(
    tableName = "assessment_choice", foreignKeys = [
        ForeignKey(
            entity = AssessmentQuestion::class,
            parentColumns = arrayOf("remoteId"),
            childColumns = arrayOf("questionId"),
            onDelete = ForeignKey.CASCADE
        )], indices = [
        Index(value = ["questionId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
@Parcelize
data class Choice(

    @PrimaryKey(autoGenerate = true)
    @SerializedName("localId")
    val localId: Int = 0,

    @ColumnInfo(name = "remoteId")
    @SerializedName("id")
    var remoteId: Int,

    @ColumnInfo
    @SerializedName("questionId")
    var questionId: Int,

    @ColumnInfo
    @SerializedName("text")
    var text: String?,

    @ColumnInfo
    @SerializedName("image_url")
    var imageUrl: String?,

    @ColumnInfo
    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @ColumnInfo
    @SerializedName("sort_order")
    var sortOrder: Int,

    @ColumnInfo
    @SerializedName("answer_order")
    val correctAnswerOrder: Int,

    @TypeConverters(TypeConverterChoiceType::class)
    @ColumnInfo
    @SerializedName("card_side")
    val column: ChoiceColumn,

    @ColumnInfo
    @SerializedName("selected_order")
    var userSelectedOrder: Int = 100,

    @ColumnInfo
    @SerializedName("is_selected_by_user")
    var isSelectedByUser: Boolean = false,

    @ColumnInfo
    @SerializedName("audio_url")
    var audioUrl: String?,

    @ColumnInfo
    @Expose
    var localAudioUrl: String?,

    @TypeConverters(
        ConvertersForDownloadStatus::class
    )
    @ColumnInfo
    @Expose
    var downloadStatus: DOWNLOAD_STATUS? = DOWNLOAD_STATUS.NOT_START,


) : Parcelable {

    constructor(
        choiceResponse: ChoiceResponse,
        questionId: Int
    ) : this(
        remoteId = choiceResponse.id,
        questionId = questionId,
        text = choiceResponse.text,
        imageUrl = choiceResponse.imageUrl,
        isCorrect = choiceResponse.isCorrect,
        sortOrder = choiceResponse.sortOrder,
        correctAnswerOrder = choiceResponse.correctAnswerOrder,
        column = choiceResponse.column,
        userSelectedOrder = choiceResponse.userSelectedOrder,
        isSelectedByUser = choiceResponse.isSelectedByUser,
        audioUrl = choiceResponse.audioUrl,
        localAudioUrl = null
    )

}
