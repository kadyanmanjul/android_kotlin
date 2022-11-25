package com.joshtalks.joshskills.common.ui.inbox.adapter

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity

data class InboxDiffCallback(
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
        return mOldInboxModelList[oldItemPosition] == mNewInboxModelList[newItemPosition]
    }


    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldInboxEntity = mOldInboxModelList[oldItemPosition]
        val newInboxEntity = mNewInboxModelList[newItemPosition]
        return oldInboxEntity.chat_id == newInboxEntity.chat_id &&
                oldInboxEntity.expiryDate == newInboxEntity.expiryDate &&
                oldInboxEntity.isCourseBought == newInboxEntity.isCourseBought &&
                oldInboxEntity.isCourseLocked == newInboxEntity.isCourseLocked
    }

}