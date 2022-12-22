package com.joshtalks.joshskills.buypage.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class SalesReasonList(
    @SerializedName("reasons")
     var reasonsList : List<String>? = null,
    @SerializedName("reason_selected")
    var reasonSelected:String?=null,
    @SerializedName("phone")
    var phoneNumber:String?=null
)