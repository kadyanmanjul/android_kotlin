package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class Card(
    @SerializedName("seq")
    val sequenceNumber: Int,

    @SerializedName("card_name")
    val cardType: String,

    @SerializedName("data")
    val data: JsonObject

)

enum class CardType(cardType: String) {
    COURSE_OVERVIEW("CourseOverview"),
    LONG_DESCRIPTION("LongDescription"),
    TEACHER_DETAILS("TeacherDetails"),
    SYLLABUS("Syllabus"),
    GUIDELINES("Guidelines"),
    DEMO_LESSON("DemoLesson"),
    REVIEWS("Reviews"),
    LOCATION_STATS("LocationStats"),
    STUDENT_FEEDBACK("StudentFeedback"),
    FAQ("FAQ"),
    ABOUT_JOSH("AboutJosh"),
    OTHER_INFO("OtherInfo")
}
