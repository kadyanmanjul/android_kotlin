package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CourseExploreModel(

    @SerializedName("amount")
    var amount: Double,

    @SerializedName("batch_type")
    val batchType: Int,

    @SerializedName("course")
    var course: Int,

    @SerializedName("created")
    val created: String,

    @SerializedName("id")
    var id: Int? = -1,

    @SerializedName("is_default")
    val isDefault: Boolean,

    @SerializedName("modified")
    val modified: String,

    @SerializedName("test_name")
    val testName: String,

    @SerializedName("thumbnail")
    var imageUrl: String,

    @SerializedName("course_name")
    var courseName: String,

    @SerializedName("course_duration")
    val courseDuration: String,

    @SerializedName("course_icon")
    var courseIcon: String,

    @SerializedName("whatsapp_url")
    var whatsappUrl: String?,

    @SerializedName("language")
    var language: String = EMPTY,

    @SerializedName("language_id")
    var languageId: Int = 1,

    @SerializedName("tag_ids")
    var tagIds: List<Int>?,

    @SerializedName("category_ids")
    var categoryIds: List<Int>?,

    @SerializedName("certificate")
    val certificate: Boolean = false,

    @SerializedName("is_clickable")
    var isClickable: Boolean = true,

    @SerializedName("card_type")
    val cardType: ExploreCardType = ExploreCardType.NORMAL

) : Parcelable {

    constructor() : this(
        amount = 0.0,
        batchType = 0,
        course = 0,
        created = EMPTY,
        id = 0,
        isDefault = false,
        modified = EMPTY,
        testName = EMPTY,
        imageUrl = EMPTY,
        courseName = EMPTY,
        courseDuration = EMPTY,
        courseIcon = EMPTY,
        whatsappUrl = EMPTY,
        language = EMPTY,
        certificate = false,
        isClickable = true,
        cardType = ExploreCardType.NORMAL,
        categoryIds = emptyList(),
        tagIds = emptyList()
    )

}
