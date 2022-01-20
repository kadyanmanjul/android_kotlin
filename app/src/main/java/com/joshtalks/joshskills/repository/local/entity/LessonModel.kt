package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class LessonModel(

    @PrimaryKey
    @SerializedName("id")
    @ColumnInfo(name = "lesson_id")
    var id: Int,

    @ColumnInfo(name = "chat_id")
    @SerializedName("chat_id") var chatId: String = "",

    @ColumnInfo(name = "lesson_no")
    @SerializedName("lesson_no")
    var lessonNo: Int,

    @ColumnInfo(name = "lesson_name")
    @SerializedName("lesson_name")
    var lessonName: String,

    @ColumnInfo(name = "thumbnail")
    @SerializedName("thumbnail")
    val thumbnailUrl: String,

    @ColumnInfo(name = "course")
    @SerializedName("course")
    var courseId: Int,

    @ColumnInfo(name = "interval")
    @SerializedName("interval")
    var interval: Int,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    var status: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "grammarStatus")
    @SerializedName("grammarStatus")
    var grammarStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "vocabularyStatus")
    @SerializedName("vocabularyStatus")
    var vocabStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "readingStatus")
    @SerializedName("readingStatus")
    var readingStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "speakingStatus")
    @SerializedName("speakingStatus")
    var speakingStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "conversationStatus")
    @SerializedName("conversationStatus")
    var conversationStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "created")
    @SerializedName("created")
    var created: Long = 0L,

    @ColumnInfo(name = "modified")
    @SerializedName("modified")
    var modified: Long = 0L,

    @ColumnInfo(name = "show_new_grammar_screen")
    @SerializedName("show_new_grammar_screen")
    var isNewGrammar: Boolean = false,

    ) : Parcelable
