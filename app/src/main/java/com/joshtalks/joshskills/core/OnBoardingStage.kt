package com.joshtalks.joshskills.core

import com.google.gson.annotations.SerializedName

enum class OnBoardingStage(val value: String) {

    @SerializedName("START_NOW_CLICKED")
    START_NOW_CLICKED("START_NOW_CLICKED"),

    @SerializedName("JI_HAAN_CLICKED")
    JI_HAAN_CLICKED("JI_HAAN_CLICKED"),

    @SerializedName("NAME_ENTERED")
    NAME_ENTERED("NAME_ENTERED"),

    @SerializedName("COURSE_OPENED")
    COURSE_OPENED("COURSE_OPENED"),
}
