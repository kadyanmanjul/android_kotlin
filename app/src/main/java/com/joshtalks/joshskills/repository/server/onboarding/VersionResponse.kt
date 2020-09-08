package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class VersionResponse(
    @SerializedName("version")
    @Expose
    var version: Version? = null,

    @SerializedName("image")
    @Expose
    var image: String? = null,

    @SerializedName("content")
    @Expose
    var content: List<Content>? = null,

    @SerializedName("maximum_number_of_interests")
    @Expose
    var maximumNumberOfInterests: Int? = null,

    @SerializedName("minimum_number_of_interests")
    @Expose
    var minimumNumberOfInterests: Int? = null,

    @SerializedName("interest_text")
    @Expose
    var interestText: String? = null,

    @SerializedName("tooltip_text")
    @Expose
    var tooltipText: String? = null,

    @SerializedName("course_interest_tags")
    @Expose
    var courseInterestTags: List<CourseInterestTag>? = null,

    @SerializedName("course_categories")
    @Expose
    var courseCategories: List<CourseCategory>? = null
)

data class CourseInterestTag(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("sort_order")
    @Expose
    var sortOrder: Int? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null
)

data class Content(
    @SerializedName("text")
    @Expose
    var text: String? = null,
    @SerializedName("description")
    @Expose
    var description: String? = null
)

data class CourseCategory(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("sort_order")
    @Expose
    var sortOrder: Int? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null
)

class Version(
    @SerializedName("id")
    @Expose
    var id: Int? = null,
    @SerializedName("name")
    @Expose
    var name: ONBOARD_VERSIONS? = ONBOARD_VERSIONS.ONBOARDING_V1
)

enum class ONBOARD_VERSIONS(val type: String) {
    ONBOARDING_V1("ONBOARDING_V1"),
    ONBOARDING_V2("ONBOARDING_V2"),
    ONBOARDING_V3("ONBOARDING_V3"),
    ONBOARDING_V4("ONBOARDING_V4")
}

