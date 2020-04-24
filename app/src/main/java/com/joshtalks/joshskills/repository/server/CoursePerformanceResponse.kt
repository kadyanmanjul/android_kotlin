package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class CoursePerformanceResponse(
    @SerializedName("complete_percent")
    val completePercent: Double,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("header")
    val header: String,
    @SerializedName("link")
    val link: String,
    @SerializedName("module_data")
    val moduleData: List<ModuleData>,
    @SerializedName("seen_video_practice")
    val seenVideoPractice: Int,
    @SerializedName("started_day")
    val startedDay: Int,
    @SerializedName("statement")
    val statement: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("total_video_practice")
    val totalVideoPractice: Int,
    @SerializedName("unlock_percent")
    val unlockPercent: Double,
    @SerializedName("certificate_detail")
    val certificateDetail: CertificateDetail
)


data class ModuleData(
    @SerializedName("module_name")
    val moduleName: String,
    @SerializedName("practice_complete")
    val practiceComplete: ArrayList<Int> = arrayListOf(),
    @SerializedName("practice_incomplete")
    val practiceIncomplete: ArrayList<Int> = arrayListOf(),
    @SerializedName("question_complete")
    val questionComplete: ArrayList<Int> = arrayListOf(),
    @SerializedName("question_incomplete")
    val questionIncomplete: ArrayList<Int> = arrayListOf(),
    @SerializedName("statement")
    val statement: String
)

@Parcelize
data class CertificateDetail(
    @SerializedName("name")
    val name: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("url")
    val url: String?
) : Parcelable