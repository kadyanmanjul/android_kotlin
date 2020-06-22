package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SyllabusData(
    @SerializedName("title")
    val title: String,

    @SerializedName("syllabus_list")
    val syllabusList: List<Syllabus>,

    @SerializedName("syllabus_url")
    val syllabusDownloadUrl: String

) : Parcelable
