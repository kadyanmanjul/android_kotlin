package com.joshtalks.joshskills.repository.local.eventbus

import com.google.gson.JsonObject
import com.joshtalks.joshskills.ui.lesson.conversationRoom.liveRooms.PubNubEvent

data class ConversationRoomPubNubEventBus(val action: PubNubEvent, val data: JsonObject)