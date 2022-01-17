package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils.getMessageTime
import com.joshtalks.joshskills.core.datetimeutils.DateTimeStyle
import com.joshtalks.joshskills.databinding.GroupChatLeftMsgBinding
import com.joshtalks.joshskills.databinding.GroupChatMetadataBinding
import com.joshtalks.joshskills.databinding.GroupChatRightMsgBinding
import com.joshtalks.joshskills.databinding.GroupChatUnreadMsgBinding
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.viewholder.ChatViewHolder

class GroupChatAdapter(diffCallback: DiffUtil.ItemCallback<ChatItem>) :
    PagingDataAdapter<ChatItem, ChatViewHolder>(
        diffCallback
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatViewHolder {
        return when (viewType) {
            RECEIVE_MESSAGE_LOCAL -> {
                val view =
                    setViewHolder<GroupChatLeftMsgBinding>(parent, R.layout.group_chat_left_msg)
                LeftChatViewHolder(view)
            }
            SENT_MESSAGE_LOCAL -> {
                val view =
                    setViewHolder<GroupChatRightMsgBinding>(parent, R.layout.group_chat_right_msg)
                RightChatViewHolder(view)
            }
            SENT_META_MESSAGE_LOCAL -> {
                val view =
                    setViewHolder<GroupChatMetadataBinding>(parent, R.layout.group_chat_metadata)
                SentMetaViewHolder(view)
            }
            RECEIVE_META_MESSAGE_LOCAL, MESSAGE_ERROR -> {
                val view =
                    setViewHolder<GroupChatMetadataBinding>(parent, R.layout.group_chat_metadata)
                MetaChatViewHolder(view)
            }
            UNREAD_MESSAGE -> {
                val view =
                    setViewHolder<GroupChatUnreadMsgBinding>(parent, R.layout.group_chat_unread_msg)
                UnreadViewHolder(view)
            }
            else -> {
                val view =
                    setViewHolder<GroupChatMetadataBinding>(parent, R.layout.group_chat_metadata)
                MetaChatViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        getItem(position)?.let { holder.bindData(it) }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.msgType!!
    }

    private fun <V : ViewDataBinding> setViewHolder(parent: ViewGroup, layoutId: Int): V {
        return DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            layoutId,
            parent,
            false
        )
    }

    inner class LeftChatViewHolder(val item: GroupChatLeftMsgBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: ChatItem) {
            item.itemData = groupChatData
        }
    }

    inner class RightChatViewHolder(val item: GroupChatRightMsgBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: ChatItem) {
            item.itemData = groupChatData
        }
    }

    inner class MetaChatViewHolder(val item: GroupChatMetadataBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: ChatItem) {
            if (groupChatData.message.isDigitsOnly())
                groupChatData.message = getMessageTime(groupChatData.message.toLong().times(1000), timeNeeded = false, DateTimeStyle.LONG)
            item.itemData = groupChatData
        }
    }

    inner class SentMetaViewHolder(val item: GroupChatMetadataBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: ChatItem) {
            groupChatData.message = groupChatData.message.replace("${groupChatData.sender} has", "You have")
            item.itemData = groupChatData
        }
    }

    inner class UnreadViewHolder(val item: GroupChatUnreadMsgBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: ChatItem) {
            item.itemData = groupChatData
        }
    }
}