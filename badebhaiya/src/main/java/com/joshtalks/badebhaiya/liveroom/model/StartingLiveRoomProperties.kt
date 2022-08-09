package com.joshtalks.badebhaiya.liveroom.model

import com.joshtalks.badebhaiya.feed.TOPIC
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse

data class StartingLiveRoomProperties(
    val isActivityOpenFromNotification: Boolean = false,
    var roomId: Int? = 0,
    var channelTopic: String = "",
    var channelName: String = "",
    var agoraUid: Int = 0,
    var moderatorId: Int? = 0,
    var token: String = "",
    var roomQuestionId: Int = 0,
    var isRoomCreatedByUser: Boolean = false,
    val isModerator: Boolean = false,
) {
    companion object {
        fun createFromRoom(room: ConversationRoomResponse, topicName: String, createdByUser: Boolean = false): StartingLiveRoomProperties{
            return StartingLiveRoomProperties(
                roomId = room.roomId,
                channelName = room.channelName!!,
                agoraUid = room.uid,
                token = room.token!!,
                isRoomCreatedByUser = if (createdByUser) true else room.isRoomCreatedByUser(),
                moderatorId = room.moderatorId,
                channelTopic = topicName,
                isModerator = room.moderatorId == room.uid
            )
        }
    }
}
