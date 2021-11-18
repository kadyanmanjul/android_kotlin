package com.joshtalks.joshskills.ui.group.viewholder

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.ui.group.model.GroupChatData

abstract class ChatViewHolder(itemView: ViewDataBinding) : RecyclerView.ViewHolder(itemView.root) {
    open fun bindData(groupChatData: GroupChatData) {}
}