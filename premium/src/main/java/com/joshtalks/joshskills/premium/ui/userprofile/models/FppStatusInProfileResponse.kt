package com.joshtalks.joshskills.premium.ui.userprofile.models

import com.google.gson.annotations.SerializedName

data class FppStatusInProfileResponse(
    @SerializedName("fpp_list")
    val fppList:List<FppDetails>?,
    @SerializedName("fpp_request")
    val fppRequest: FppRequest?
    )

data class FppRequest (
    @SerializedName("text")
    val text:String?,
    @SerializedName("request_status")
    val requestStatus:String?,
    @SerializedName("group_id")
    val groupId: String?,
    @SerializedName("agora_uid")
    val agoraUid: Int,
    @SerializedName("can_send_message")
    val canSendMessage: Boolean = false,
)
data class FppDetails(
    @SerializedName("mentor_id")
    val partnerMentorId:String?,
    @SerializedName("photo_url")
    val photoUrl:String?,
    @SerializedName("name")
    val fullName:String?,
    @SerializedName("text")
    val text:String?,
)