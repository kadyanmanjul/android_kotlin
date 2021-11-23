package com.joshtalks.joshskills.ui.group.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.ui.group.model.ChatItem

object GroupChatComparator : DiffUtil.ItemCallback<ChatItem>() {
    override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem.message_id == newItem.message_id
    }

    override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
        return oldItem.message == newItem.message
    }
}