package com.joshtalks.joshskills.ui.group.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.base.local.entity.group.GroupItemData

object GroupItemComparator : DiffUtil.ItemCallback<GroupItemData>() {
    override fun areItemsTheSame(oldItem: GroupItemData, newItem: GroupItemData): Boolean {
        return oldItem.getUniqueId() == newItem.getUniqueId()
    }

    override fun areContentsTheSame(oldItem: GroupItemData, newItem: GroupItemData): Boolean {
        return oldItem.getTitle() == newItem.getTitle() &&
                oldItem.getSubTitle() == newItem.getSubTitle() &&
                oldItem.getLastMessageTime() == newItem.getLastMessageTime() &&
                oldItem.getImageUrl() == newItem.getImageUrl() &&
                oldItem.getUnreadMsgCount() == newItem.getUnreadMsgCount()
    }
}