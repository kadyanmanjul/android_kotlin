package com.joshtalks.joshskills.common.repository.server.onboarding

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.ONBOARDING_VERSION_KEY
import com.joshtalks.joshskills.common.core.PrefManager

open class VersionResponse {

    @SerializedName("version")
    @Expose
    var version: Version = Version()

    @SerializedName("image")
    @Expose
    var image: String? = null

    @SerializedName("content")
    @Expose
    var content: List<Content>? = null

    @SerializedName("maximum_number_of_interests")
    @Expose
    var maximumNumberOfInterests: Int? = null

    @SerializedName("minimum_number_of_interests")
    @Expose
    var minimumNumberOfInterests: Int? = null

    @SerializedName("interest_text")
    @Expose
    var interestText: String? = null

    @SerializedName("tooltip_text")
    @Expose
    var tooltipText: String? = null

    @SerializedName("course_interest_tags")
    @Expose
    var courseInterestTags: List<CourseInterestTag>? = null

    @SerializedName("course_categories")
    @Expose
    var courseCategories: List<CourseCategory>? = null

    @SerializedName("course_headings")
    @Expose
    var course_headings: List<CourseHeading>? = null

    @SerializedName("v5_title")
    @Expose
    var v5Title: String? = null

    @SerializedName("v5_description")
    @Expose
    var v5Description: String? = null

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    companion object {
        @JvmStatic
        private var instance: VersionResponse? = null

        @JvmStatic
        fun getInstance(): VersionResponse {
            return try {
                instance = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(ONBOARDING_VERSION_KEY), VersionResponse::class.java
                )
                instance!!
            } catch (ex: Exception) {
                VersionResponse()
            }
        }

        fun update(value: VersionResponse) {
            PrefManager.put(ONBOARDING_VERSION_KEY, AppObjectController.gsonMapper.toJson(value))
        }
    }

    fun hasVersion(): Boolean {
        return version != null && version.name != null
    }
}

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

data class CourseHeading(
    @SerializedName("id")
    @Expose
    var id: Int? = null,

    @SerializedName("sort_order")
    @Expose
    var sortOrder: Int? = null,

    @SerializedName("name")
    @Expose
    var name: String? = null,

    var isSelected: Boolean = false
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
    ONBOARDING_V4("ONBOARDING_V4"),
    ONBOARDING_V5("ONBOARDING_V5"),
    ONBOARDING_V6("ONBOARDING_V6"),
    ONBOARDING_V7("ONBOARDING_V7"),
    ONBOARDING_V8("ONBOARDING_V8"),

}
