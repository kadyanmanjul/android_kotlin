package com.joshtalks.badebhaiya.pubnub

import android.os.Message
import androidx.collection.ArraySet
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomPubNubEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
    This Object provides data related to pubnub which can be consumed by view models and convert to states
    using Live Data or StateFlow.
*/

object PubNubData {

    internal val _speakersList = MutableSharedFlow<ArraySet<LiveRoomUser>>()
    val speakerList = _speakersList.asSharedFlow()

    internal val _audienceList = MutableSharedFlow<ArraySet<LiveRoomUser>>()
    val audienceList = _audienceList.asSharedFlow()

    internal val _liveEvent = MutableSharedFlow<Message>(
        replay = Int.MAX_VALUE
    )
    val liveEvent = _liveEvent.asSharedFlow()

    val pubNubEvents = MutableSharedFlow<ConversationRoomPubNubEventBus>(
        replay = Int.MAX_VALUE
    )

}