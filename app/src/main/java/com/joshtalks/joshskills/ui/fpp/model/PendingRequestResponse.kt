package com.joshtalks.joshskills.ui.fpp.model

import com.google.gson.annotations.SerializedName

data class PendingRequestResponse (
    @SerializedName("pending_requests")
    val pendingRequestsList:List<PendingRequestDetail>
    )

data class PendingRequestDetail (
    @SerializedName("sender_mentor_id")
    val senderMentorId:String?,
    @SerializedName("photo_url")
    val photoUrl:String?,
    @SerializedName("full_name")
    val fullName:String?,
    @SerializedName("text")
    val textToShow:String?,
    @SerializedName("request_status")
    val requestStatus:String?,
    )
