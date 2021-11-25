package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.GroupChatLeftMsgBinding
import com.joshtalks.joshskills.databinding.GroupChatMetadataBinding
import com.joshtalks.joshskills.databinding.GroupChatRightMsgBinding
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
            SENT_META_MESSAGE_LOCAL, RECEIVE_META_MESSAGE_LOCAL, MESSAGE_ERROR -> {
                val view =
                    setViewHolder<GroupChatMetadataBinding>(parent, R.layout.group_chat_metadata)
                MetaChatViewHolder(view)
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
            item.itemData = groupChatData
        }
    }
}

//private const val LEFT_TEXT_MESSAGE = 1  //Message is received
//private const val RIGHT_TEXT_MESSAGE = 2 //Message is sent
//private const val META_DATA_MESSAGE = 3  //Message is of type metadata
//private const val ERROR_TEXT_MESSAGE = 0  //Message type is not known