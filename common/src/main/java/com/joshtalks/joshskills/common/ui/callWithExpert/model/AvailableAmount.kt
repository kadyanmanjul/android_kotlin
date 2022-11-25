package com.joshtalks.joshskills.common.ui.callWithExpert.model

import com.google.gson.annotations.SerializedName

data class AvailableAmount(
    val amount_list: List<Amount>,
    @SerializedName("common_test_id")
    val commonTestId: Int
)