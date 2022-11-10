package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class SalesReasonList(
    @SerializedName("reasons")
     var reasonsList : List<String>? = null
)