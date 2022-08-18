package com.joshtalks.badebhaiya.pubnub

import android.os.Message
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
    This Object provides data related to pubnub which can be consumed by view models and convert to states
    using Live Data or StateFlow.
*/

object PubNubData {

    internal val _speakersList = MutableSharedFlow<List<LiveRoomUser>>()
    val speakerList = _speakersList.asSharedFlow()

    internal val _audienceList = MutableSharedFlow<List<LiveRoomUser>>()
    val audienceList = _audienceList.asSharedFlow()

    internal val _liveEvent = MutableSharedFlow<Message>(
        replay = Int.MAX_VALUE
    )
    val liveEvent = _liveEvent.asSharedFlow()

    val pubNubEvents = MutableSharedFlow<ConversationRoomPubNubEventBus>(
        replay = Int.MAX_VALUE
    )

    internal val _pubNubState = MutableSharedFlow<PubNubState>()
    val pubNubState = _pubNubState.asSharedFlow()

    val eventsMap = ConcurrentHashMap<Long, Long>()


}