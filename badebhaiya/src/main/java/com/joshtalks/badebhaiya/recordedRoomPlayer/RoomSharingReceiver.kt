package com.joshtalks.badebhaiya.recordedRoomPlayer

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOSEN_COMPONENT
import com.joshtalks.badebhaiya.feed.model.UserDeeplink
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import timber.log.Timber

/**
     This class is to know if the user has shared the room.
*/

class RoomSharingReceiver: BroadcastReceiver(){

    companion object {
        const val ROOM_SHARING = 456

        var deeplinkRequest: UserDeeplink? = null
    }

    private val conversationRoomRepository by lazy {
        ConversationRoomRepository()
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        val clickedComponent : ComponentName? = p1?.getParcelableExtra(EXTRA_CHOSEN_COMPONENT)
        Timber.tag("roomsharing").d("ROOM SHARED AND DATA => ${clickedComponent?.packageName}")
        deeplinkRequest?.let {
            conversationRoomRepository.userDeeplink(it)
        }
    }

}