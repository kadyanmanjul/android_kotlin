package com.joshtalks.joshskills.premium.repository.server

import com.google.gson.annotations.SerializedName

data class ChooseLanguages(
    @SerializedName("test_id")
    val testId: String = "",

    @SerializedName("language")
    val languageName: String = "",
)