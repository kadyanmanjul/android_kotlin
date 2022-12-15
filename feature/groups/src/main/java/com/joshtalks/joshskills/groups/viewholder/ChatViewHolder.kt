package com.joshtalks.joshskills.groups.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.repository.local.entity.groups.ChatItem

abstract class ChatViewHolder(itemView: ViewDataBinding) : RecyclerView.ViewHolder(itemView.root) {
    open fun bindData(groupChatData: ChatItem) {}
}