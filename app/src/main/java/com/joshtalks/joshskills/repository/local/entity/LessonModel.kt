package com.joshtalks.joshskills.repository.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity
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
    var status: String
)