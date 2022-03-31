package com.joshtalks.badebhaiya.feed.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class RoomListResponse(
    @SerializedName("live_room")
    val liveRoomList: List<RoomListResponseItem>?,

    /*@SerializedName("schedule_room")
    val scheduledRoomList: List<RoomListResponseItem>?*/
)


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
    @SerializedName("start_time")
    val startTime: String?,
    @SerializedName("ended")
    val endTime: String?,
    @SerializedName("is_scheduled")
    val isScheduled: Boolean?,
    @SerializedName("speakers_data")
    val speakersData: SpeakerData?,
    var conversationRoomQuestionId: Int? = null
)

data class SpeakerData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
)

@Parcelize
data class LiveRoomUser(
    @SerializedName("id")
    var id: Int?,
    @SerializedName("is_speaker")
    var isSpeaker: Boolean?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("photo_url")
    var photoUrl: String?,
    @SerializedName("sort_order")
    var sortOrder: Int?,
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