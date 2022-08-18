package com.joshtalks.badebhaiya.feed.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.utils.UniqueList
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

data class RoomListResponse(
    @SerializedName("live_room")
    val liveRoomList: List<RoomListResponseItem>?,

    @SerializedName("schedule_room")
    val scheduledRoomList: List<RoomListResponseItem>?,

    @SerializedName("current_time")
    var currentTime: Long,

    @SerializedName("is_speaker")
    val isSpeaker:Boolean?
)

@Parcelize
data class RoomListResponseItem(
    @SerializedName("id")
    val roomId: Int,
    @SerializedName("audience_count")
    var audienceCount: String?,
    @SerializedName("channel_name")
    val channelId: String?,
    @SerializedName("speaker_count")
    var speakerCount: String?,
    @SerializedName("created")
    val created: Long?,
    @SerializedName("started_by")
    val startedBy: Int?,
    @SerializedName("top_user_list")
    var liveRoomUserList: ArrayList<LiveRoomUser>?,
    @SerializedName("topic")
    val topic: String?,
    @SerializedName("start_time")
    var startTime: Long?,
    @SerializedName("ended")
    val endTime: String?,
    @SerializedName("is_scheduled")
    var isScheduled: Boolean?,
    @SerializedName("speakers_data")
    val speakersData: SpeakerData?,
    @SerializedName("room_recordings")
    val recordings:  List<RecordedResponse>?,
    @SerializedName("current_time")
    var currentTime: Long = 0,
    @SerializedName("previous_room_id")
    val previousRoomId: Int,
    @SerializedName("previous_room_topic")
    val previousRoomTopic: String?="",
    var conversationRoomQuestionId: Int? = null,
    var conversationRoomType: ConversationRoomType? = null,
    @SerializedName("users_count")
    var users_count: Int? = null
) : Parcelable {
    val startTimeDate: Long = startTime ?: 0
        /*get() {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ZonedDateTime.parse(startTime).toEpochSecond() * 1000
                } else {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(startTime).time
                }
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }*/

    fun getStartDate(): String{
//        DateTimeUtils.date
        val date = Utils.getMessageTime((startTime ?: 0L), false, DateTimeStyle.LONG)
        Timber.tag("audiostarttime").d("START DATE IS => $startTime AND FORMATTED DATE => $date")
        return date
    }

    fun getStartTime(): String {
        val time = Utils.getMessageTimeInHours(Date(startTime ?: 0))
        Timber.tag("audiostarttime").d("START TIME IS => $startTime AND FORMATTED TIME => $time")
        return time
    }

    fun displayStartDateTime() : String {
        return "${getStartDate()} at ${getStartTime()}"
    }
}

enum class ConversationRoomType() {
    LIVE,
    NOT_SCHEDULED,
    SCHEDULED,
    RECORDED;
}

data class RecordedResponseList(
    @SerializedName("recorded_data")
    val recordings:List<RoomListResponseItem>
)

data class GuestUser(
    @SerializedName("user_id")
    val userId:String,
    @SerializedName("token")
    val token:String
)

data class LinkUser(
    @SerializedName("user")
    val userId: String,
    @SerializedName("utm_term")
    val term:String,
    @SerializedName("utm_medium")
    val medium:String,
    @SerializedName("utm_source")
    val source:String

)

@Parcelize
data class RecordedResponse(
    @SerializedName("id")
    val id:Int,
    @SerializedName("recording_file")
    val url:String
):Parcelable

data class RecordedRoomItem(
    @SerializedName("id")
    val roomId: Int,
    @SerializedName("started_by")
    val startedBy: Int?,
    @SerializedName("topic")
    val topic: String?,
    @SerializedName("start_time")
    var startTime: Long?,
    @SerializedName("ended")
    val endTime: String?,
    @SerializedName("speakers_data")
    val speakersData: SpeakerData?,
)

@Parcelize
data class SpeakerData(
    @SerializedName("id")
    val id: Int,
    @SerializedName("user_id")
    val userId: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("uuid")
    val uuid: String?,
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("short_name")
    val shortName: String?
)  : Parcelable

@Parcelize
data class LiveRoomUser(
    @SerializedName("id")
    var id: Int?,
    @SerializedName("is_speaker")
    var isSpeaker: Boolean?,
    @SerializedName("short_name")
    var name: String?,
    @SerializedName("full_name")
    var fullName: String?,
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
    @SerializedName("user_id")
    var userId: String = EMPTY,
    var isInviteSent: Boolean = false,
    var isSpeakerAccepted: Boolean = false
) : Parcelable
