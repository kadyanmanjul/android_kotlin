package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
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

) : Parcelable {
    constructor(assessmentIntro: AssessmentIntro) : this(
        type = assessmentIntro.type,
        title = assessmentIntro.title,
        description = assessmentIntro.description,
        imageUrl = assessmentIntro.imageUrl
    )
}
