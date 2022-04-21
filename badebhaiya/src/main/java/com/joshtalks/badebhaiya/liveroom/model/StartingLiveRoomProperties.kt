package com.joshtalks.badebhaiya.liveroom.model

import com.joshtalks.badebhaiya.feed.TOPIC
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse

data class StartingLiveRoomProperties(
    val isActivityOpenFromNotification: Boolean = false,
    val roomId: Int,
    val channelTopic: String,
    val channelName: String,
    val agoraUid: Int,
    val moderatorId: Int,
    val token: String,
    val roomQuestionId: Int = 0,
    val isRoomCreatedByUser: Boolean = false
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
            )
        }
    }
}
