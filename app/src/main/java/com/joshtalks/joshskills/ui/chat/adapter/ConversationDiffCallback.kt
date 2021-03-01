package com.joshtalks.joshskills.ui.inbox.adapter

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.repository.local.entity.ChatModel

data class ConversationDiffCallback(
    private val mOldInboxModelList: List<ChatModel>,
    private val mNewInboxModelList: List<ChatModel>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return mOldInboxModelList.size
    }


    override fun getNewListSize(): Int {
        return mNewInboxModelList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldInboxModelList[oldItemPosition] == mNewInboxModelList[newItemPosition]
    }


    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldMovie = mOldInboxModelList[oldItemPosition]
        val newMovie = mNewInboxModelList[newItemPosition]
        return oldMovie.chatId == newMovie.chatId
    }

}