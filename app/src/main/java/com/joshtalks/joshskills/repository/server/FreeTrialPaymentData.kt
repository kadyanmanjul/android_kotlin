package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FreeTrialPaymentData(

    @SerializedName("course_name")
    val courseName: String = EMPTY,

    @SerializedName("teacher_name")
    val teacherName: String = EMPTY,

    @SerializedName("image_url")
    val imageUrl: String = EMPTY,

    @SerializedName("actual_amount")
    val actualAmount: String = EMPTY,

    @SerializedName("discount")
    val discount: String = EMPTY,

    @SerializedName("heading")
    val heading: String = EMPTY,

    @SerializedName("rating")
    val rating: Double = 0.0,

    @SerializedName("ratings_count")
    val ratingsCount: Int = 0,

    @SerializedName("savings")
    val savings: String = EMPTY,

    @SerializedName("sub_headings")
    val subHeadings: List<String> = emptyList(),

    @SerializedName("encrypted_text")
    val encryptedText: String = EMPTY

) : Parcelable
