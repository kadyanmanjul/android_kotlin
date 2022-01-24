package com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.ui.lesson.conversationRoom.model.LiveRoomUser

data class ConversationUserDiffCallback(
    private val mOldUserList: List<LiveRoomUser>,
    private val mNewUserList: List<LiveRoomUser>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return mOldUserList.size
    }
    override fun getNewListSize(): Int {
        return mNewUserList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {

        return mOldUserList[oldItemPosition] == mNewUserList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldUser = mOldUserList[oldItemPosition]
        val newUser = mNewUserList[newItemPosition]

        return (oldUser.id == newUser.id &&
                oldUser.isSpeaker == newUser.isSpeaker &&
                oldUser.name == newUser.name &&
                oldUser.isMicOn.xor(newUser.isMicOn) &&
                oldUser.isHandRaised.xor(newUser.isHandRaised) &&
                oldUser.sortOrder == newUser.sortOrder &&
                oldUser.photoUrl ==  newUser.photoUrl )
    }

}