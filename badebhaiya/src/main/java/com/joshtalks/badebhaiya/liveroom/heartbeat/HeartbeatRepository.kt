package com.joshtalks.badebhaiya.liveroom.heartbeat

import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.repository.service.ConversationRoomNetworkService
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject

class HeartbeatRepository @Inject constructor(
    private val roomApi: ConversationRoomNetworkService
) {

    val heartbeat = flow {
        while (true) {
            emit(
                roomApi.triggerHeartbeat(
                    Heartbeat(
                        roomId = PubNubManager.getLiveRoomProperties().roomId
                    )
                )
            )
            delay(REFRESHING_INTERVAL)
        }
    }

    companion object {
        const val REFRESHING_INTERVAL = 5000L
    }
}