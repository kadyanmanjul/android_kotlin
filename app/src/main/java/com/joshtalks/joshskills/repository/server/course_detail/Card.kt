package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class Card(
    @SerializedName("seq")
    val sequenceNumber: Int,

    @SerializedName("card_name")
    val cardType: CardType,

    @SerializedName("data")
    val data: JsonObject

)

enum class CardType {
    @SerializedName("CourseOverview")
    COURSE_OVERVIEW,
    @SerializedName("LongDescription")
    LONG_DESCRIPTION,
    @SerializedName("TeacherDetails")
    TEACHER_DETAILS,
    @SerializedName("Syllabus")
    SYLLABUS,
    @SerializedName("Guidelines")
    GUIDELINES,
    @SerializedName("DemoLesson")
    DEMO_LESSON,
    @SerializedName("Reviews")
    REVIEWS,
    @SerializedName("LocationStats")
    LOCATION_STATS,
    @SerializedName("StudentFeedback")
    STUDENT_FEEDBACK,
    @SerializedName("FAQ")
    FAQ,
    @SerializedName("AboutJosh")
    ABOUT_JOSH,
    @SerializedName("OtherInfo")
    OTHER_INFO
}
