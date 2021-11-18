package com.joshtalks.joshskills.ui.group.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.ui.group.model.GroupChatData

object GroupChatComparator : DiffUtil.ItemCallback<GroupChatData>() {
    override fun areItemsTheSame(oldItem: GroupChatData, newItem: GroupChatData): Boolean {
        return oldItem.getUniqueId() == newItem.getUniqueId()
    }

    override fun areContentsTheSame(oldItem: GroupChatData, newItem: GroupChatData): Boolean {
        return oldItem.getTitle() == newItem.getTitle()
    }
}