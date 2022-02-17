package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class FppStatusInProfileResponse(
    @SerializedName("fpp_list")
    val fppList:List<FppDetails>?,
    @SerializedName("fpp_request")
    val fppRequest:FppRequest?
    )

data class FppRequest (
    @SerializedName("text")
    val text:String?,
    @SerializedName("request_status")
    val requestStatus:String?
)
data class FppDetails(
    @SerializedName("partner_mentor_id")
    val partnerMentorId:String?,
    @SerializedName("photo_url")
    val photoUrl:String?,
    @SerializedName("full_name")
    val fullName:String?,
    @SerializedName("text")
    val text:String?,
    @SerializedName("request_status")
    val requestStatus:String?
)