package com.joshtalks.joshskills.premium.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentIntroResponse(

    @SerializedName("type")
    val type: ChoiceType,

    @SerializedName("title")
    val title: String? = EMPTY,

    @SerializedName("description")
    val description: String? = EMPTY,

    @SerializedName("image_url")
    val imageUrl: String? = EMPTY

) : Parcelable