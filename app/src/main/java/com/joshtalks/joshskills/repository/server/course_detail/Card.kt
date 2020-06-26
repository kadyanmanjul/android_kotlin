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

enum class CardType(val type: String) {
    @SerializedName("CourseOverview")
    COURSE_OVERVIEW("CourseOverview"),

    @SerializedName("LongDescription")
    LONG_DESCRIPTION("LongDescription"),

    @SerializedName("TeacherDetails")
    TEACHER_DETAILS("TeacherDetails"),

    @SerializedName("Syllabus")
    SYLLABUS("Syllabus"),

    @SerializedName("Guidelines")
    GUIDELINES("Guidelines"),

    @SerializedName("DemoLesson")
    DEMO_LESSON("DemoLesson"),

    @SerializedName("Reviews")
    REVIEWS("Reviews"),

    @SerializedName("LocationStats")
    LOCATION_STATS("LocationStats"),

    @SerializedName("StudentFeedback")
    STUDENT_FEEDBACK("StudentFeedback"),

    @SerializedName("FAQ")
    FAQ("FAQ"),

    @SerializedName("AboutJosh")
    ABOUT_JOSH("AboutJosh"),

    @SerializedName("OtherInfo")
    OTHER_INFO("OtherInfo")
}
