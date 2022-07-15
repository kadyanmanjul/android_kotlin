package com.joshtalks.joshskills.ui.group.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.base.local.entity.group.ChatItem

object GroupChatComparator : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem.groupId == newItem.groupId && oldItem.msgTime == newItem.msgTime
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem.message == newItem.message && oldItem.sender == newItem.sender
    }
}