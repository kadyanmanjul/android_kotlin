package com.joshtalks.joshskills.repository.local.eventbus

import com.google.gson.JsonObject

data class ConversationRoomListPubNubEventBus(val action: String, val data: JsonObject)