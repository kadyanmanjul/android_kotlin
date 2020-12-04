package com.joshtalks.joshskills.repository.local.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class LessonModel(

    @PrimaryKey
    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "lesson_id")
    var id: Int,

    @ColumnInfo(name = "lesson_no")
    @SerializedName("lesson_no")
    @Expose
    var lessonNo: Int,

    @ColumnInfo(name = "lesson_name")
    @SerializedName("lesson_name")
    @Expose
    var lessonName: String,

    @ColumnInfo(name = "thumbnail")
    @SerializedName("thumbnail")
    @Expose
    val varthumbnail: String,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    @Expose
    var status: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "course")
    @SerializedName("course")
    @Expose
    var course: Int,

    @ColumnInfo(name = "interval")
    @SerializedName("interval")
    @Expose
    var interval: Int,
    @ColumnInfo(name = "grammarStatus")
    @SerializedName("grammarStatus")
    @Expose
    var grammarStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "vocabularyStatus")
    @SerializedName("vocabularyStatus")
    @Expose
    var vocabStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "readingStatus")
    @SerializedName("readingStatus")
    @Expose
    var readingStatus: LESSON_STATUS? = LESSON_STATUS.NO,

    @ColumnInfo(name = "speakingStatus")
    @SerializedName("speakingStatus")
    @Expose
    var speakingStatus: LESSON_STATUS? = LESSON_STATUS.NO
) : Parcelable