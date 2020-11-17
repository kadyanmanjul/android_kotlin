package com.joshtalks.joshskills.repository.server.course_overview

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class CourseOverviewBaseResponse(
    @SerializedName("message")
    @Expose
    var message: String? = null,
    @SerializedName("response_data")
    @Expose
    val responseData: List<CourseOverviewResponse>? = null,

    @SerializedName("Success")
    @Expose
    val success: Boolean,
    @SerializedName("pdf_info")
    @Expose
    val pdfInfo: PdfInfo,

    )

data class CourseOverviewResponse(

    @SerializedName("title")
    @Expose
    var title: String,
    @SerializedName("total_count")
    @Expose
    var totalCount: Int,
    @SerializedName("data")
    @Expose
    var data: List<CourseOverviewItem>
)

data class CourseOverviewItem(

    @SerializedName("lesson_id")
    @Expose
    var lessonId: Int,
    @SerializedName("lesson_no")
    @Expose
    var lessonNo: Int,
    @SerializedName("lesson_name")
    @Expose
    var lessonName: String,
    @SerializedName("status")
    @Expose
    var status: String,
    @SerializedName("grammar_percentage")
    @Expose
    var grammarPercentage: String,
    @SerializedName("rp_percentage")
    @Expose
    var rpPercentageval: String,
    @SerializedName("vp_percentage")
    @Expose
    var vpPercentage: String,
    @SerializedName("speaking_percentage")
    @Expose
    var speakingPercentage: String
)

data class PdfInfo(

    @SerializedName("course_pdf_url")
    @Expose
    val coursePdfUrl: String = EMPTY,
    @SerializedName("pdf_name")
    @Expose
    val coursePdfName: String = EMPTY,
    @SerializedName("pdf_page_count")
    @Expose
    val coursePdfPageCount: String = EMPTY,
    @SerializedName("pdf_size")
    @Expose
    val coursePdfSize: String = EMPTY,
)
