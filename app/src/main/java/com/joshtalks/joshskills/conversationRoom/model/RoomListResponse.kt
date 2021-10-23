package com.joshtalks.joshskills.conversationRoom.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

class RoomListResponse : ArrayList<RoomListResponseItem>()

data class RoomListResponseItem(
    @SerializedName("id")
    val roomId: String,
    @SerializedName("audience_count")
    var audienceCount: String?,
    @SerializedName("channel_name")
    val channelId: String?,
    @SerializedName("speaker_count")
    var speakerCount: String?,
    @SerializedName("started_by")
    val startedBy: Int?,
    @SerializedName("top_user_list")
    var liveRoomUserList: ArrayList<LiveRoomUser>?,
    @SerializedName("topic")
    val topic: String?,
    var conversationRoomQuestionId:Int?=null
)
@Parcelize
data class LiveRoomUser(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("is_speaker")
    var isSpeaker: Boolean?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("is_moderator")
    var isModerator: Boolean = false,
    @SerializedName("is_mic_on")
    var isMicOn: Boolean = false,
    @SerializedName("is_speaking")
    var isSpeaking: Boolean = false,
    @SerializedName("is_hand_raised")
    var isHandRaised: Boolean = false,
    @SerializedName("mentor_id")
    var mentorId: String,
    var isInviteSent: Boolean = false
) : Parcelable