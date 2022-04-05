package com.joshtalks.badebhaiya.liveroom.model

import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.liveroom.adapter.PubNubEvent

data class ConversationRoomPubNubEventBus(val action: PubNubEvent, val data: JsonObject)