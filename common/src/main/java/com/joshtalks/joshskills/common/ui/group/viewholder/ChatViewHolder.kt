package com.joshtalks.joshskills.common.ui.group.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.ui.group.model.ChatItem

abstract class ChatViewHolder(itemView: ViewDataBinding) : RecyclerView.ViewHolder(itemView.root) {
    open fun bindData(groupChatData: ChatItem) {}
}