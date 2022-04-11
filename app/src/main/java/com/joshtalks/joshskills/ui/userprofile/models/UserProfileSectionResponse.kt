package com.joshtalks.joshskills.ui.userprofile.models

import com.google.gson.annotations.SerializedName

data class UserProfileSectionResponse(
    @SerializedName("success")
    val success:Boolean?=null,
    @SerializedName("section_impression_id")
    val sectionImpressionId: String?=null

)