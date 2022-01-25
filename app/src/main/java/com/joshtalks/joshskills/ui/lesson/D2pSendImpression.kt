package com.joshtalks.joshskills.ui.lesson

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class D2pSendImpression(
    @SerializedName("mentor_id")
    val mentorId: String,

    @SerializedName("open_speak_fragment")
    val speakingTabClicked: Boolean? = null,

    @SerializedName("continue_video_button")
    val startedPlayingVideo: Boolean? = null,

    @SerializedName("time_spent_video")
    val videoTimeDuration: Long? = null,

    @SerializedName("call_clicked")
    val callBtnClicked: Boolean? = null,

    @SerializedName("time_spent_call")
    val timeSpentOnCall: Int? = null,

    @SerializedName("second_section_clicked")
    val howToSpeakClicked: Boolean? = null

) : Parcelable
