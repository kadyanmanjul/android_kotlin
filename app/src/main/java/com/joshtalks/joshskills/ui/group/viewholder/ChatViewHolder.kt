package com.joshtalks.joshskills.ui.group.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.base.local.entity.group.ChatItem

abstract class ChatViewHolder(itemView: ViewDataBinding) : RecyclerView.ViewHolder(itemView.root) {
    open fun bindData(groupChatData: ChatItem) {}
}