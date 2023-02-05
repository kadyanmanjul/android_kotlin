package com.joshtalks.joshskills.premium.ui.callWithExpert.model

import com.google.gson.annotations.SerializedName

data class ExpertUpgradeDetails(

    @SerializedName("amount")
    val amount: Int,

    @SerializedName("upgrade_text")
    val upgradeText: String,

    @SerializedName("features")
    val features: List<String>,

    @SerializedName("premium_test_id")
    val testId: Int,
)
