package com.joshtalks.joshskills.repository.local.eventbus

import com.google.gson.JsonObject

data class ConversationRoomPubNubEventBus(val action: String, val data: JsonObject)