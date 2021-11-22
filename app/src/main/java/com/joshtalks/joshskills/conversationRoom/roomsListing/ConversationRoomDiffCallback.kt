package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.util.Log
import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.conversationRoom.model.RoomListResponseItem

data class ConversationRoomDiffCallback(
    private val mOldRoomList: List<RoomListResponseItem>,
    private val mNewRoomList: List<RoomListResponseItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return mOldRoomList.size
    }
    override fun getNewListSize(): Int {
        return mNewRoomList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        return mOldRoomList[oldItemPosition] == mNewRoomList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldRoom = mOldRoomList[oldItemPosition]
        val newRoom = mNewRoomList[newItemPosition]

        return (oldRoom.channelId == newRoom.channelId &&
                oldRoom.audienceCount == newRoom.audienceCount &&
                oldRoom.speakerCount == newRoom.speakerCount &&
                oldRoom.liveRoomUserList ==  newRoom.liveRoomUserList )
    }

}