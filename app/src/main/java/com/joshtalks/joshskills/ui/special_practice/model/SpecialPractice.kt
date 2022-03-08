package com.joshtalks.joshskills.ui.special_practice.model


import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "special_table")
@Parcelize
data class SpecialPractice(

    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "special_id")
    val id: Int?,

    @ColumnInfo(name = "created")
    @SerializedName("created")
    val created: String?,

    /*@ColumnInfo(name = "chat_id")
    @SerializedName("chat_id")
    var chatId: String = "",*/

    @ColumnInfo(name = "image_url")
    @SerializedName("image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "instruction_text")
    @SerializedName("instruction_text")
    val instructionText: String?,

    @ColumnInfo(name = "main_text")
    @SerializedName("main_text")
    val mainText: String?,

    @ColumnInfo(name = "modified")
    @SerializedName("modified")
    val modified: String?,

    @ColumnInfo(name = "practice_no")
    @SerializedName("practice_no")
    val practiceNo: Int?,

    @ColumnInfo(name = "sample_video_url")
    @SerializedName("sample_video_url")
    val sampleVideoUrl: String?,

    @ColumnInfo(name = "word_text")
    @SerializedName("word_text")
    val wordText: String?,

    @ColumnInfo(name ="sentence_en")
    @SerializedName("sentence_en")
    val sentenceEnglish:String?,

    @ColumnInfo(name ="word_en")
    @SerializedName("word_en")
    val wordEnglish:String?,

    @ColumnInfo(name = "sentence_hi")
    @SerializedName("sentence_hi")
    val sentenceHindi :String?,

    @ColumnInfo(name = "word_hi")
    @SerializedName("word_hi")
    val wordHindi :String?

): Parcelable