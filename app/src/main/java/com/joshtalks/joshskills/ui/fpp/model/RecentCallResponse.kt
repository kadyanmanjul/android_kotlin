package com.joshtalks.joshskills.ui.fpp.model

import com.google.gson.annotations.SerializedName

data class RecentCallResponse(@SerializedName("recent_calls_list") var arrayList: ArrayList<RecentCall>)

data class RecentCall(
    @SerializedName("first_name") var firstName: String?,
    @SerializedName("last_name") var lastName: String?,
    @SerializedName("photo_url") var photoUrl: String?,
    @SerializedName("call_duration") var callDuration: Int?,
    @SerializedName("fpp_request_status") var fppRequestStatus:String
)