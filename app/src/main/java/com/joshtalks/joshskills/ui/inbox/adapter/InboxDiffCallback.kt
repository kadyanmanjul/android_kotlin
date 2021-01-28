package com.joshtalks.joshskills.ui.inbox.adapter

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity

class InboxDiffCallback(
    private val mOldInboxModelList: List<InboxEntity>,
    private val mNewInboxModelList: List<InboxEntity>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return mOldInboxModelList.size
    }


    override fun getNewListSize(): Int {
        return mNewInboxModelList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldInboxModelList[oldItemPosition].conversation_id == mNewInboxModelList[newItemPosition].conversation_id
    }


    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldMovie = mOldInboxModelList[oldItemPosition]
        val newMovie = mNewInboxModelList[newItemPosition]
        return oldMovie.chat_id == newMovie.chat_id
    }

}