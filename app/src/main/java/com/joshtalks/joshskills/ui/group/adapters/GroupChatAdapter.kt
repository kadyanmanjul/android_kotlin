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
import com.joshtalks.joshskills.ui.group.model.GroupChatData
import com.joshtalks.joshskills.ui.group.model.MessageType
import com.joshtalks.joshskills.ui.group.viewholder.ChatViewHolder

class GroupChatAdapter(diffCallback: DiffUtil.ItemCallback<GroupChatData>) :
    PagingDataAdapter<GroupChatData, ChatViewHolder>(
        diffCallback
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatViewHolder {
        return when (viewType) {
            LEFT_TEXT_MESSAGE -> {
                val view =
                    setViewHolder<GroupChatLeftMsgBinding>(parent, R.layout.group_chat_left_msg)
                LeftChatViewHolder(view)
            }
            RIGHT_TEXT_MESSAGE -> {
                val view =
                    setViewHolder<GroupChatRightMsgBinding>(parent, R.layout.group_chat_right_msg)
                RightChatViewHolder(view)
            }
            META_DATA_MESSAGE, ERROR_TEXT_MESSAGE -> {
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
        return when (getItem(position)?.getType()) {
            MessageType.META_DATA -> META_DATA_MESSAGE
            MessageType.SENT_MESSAGE -> RIGHT_TEXT_MESSAGE
            MessageType.RECEIVED_MESSAGE -> LEFT_TEXT_MESSAGE
            MessageType.DATA_ERROR -> ERROR_TEXT_MESSAGE
            else -> ERROR_TEXT_MESSAGE
        }
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
        override fun bindData(groupChatData: GroupChatData) {
            item.itemData = groupChatData
        }
    }

    inner class RightChatViewHolder(val item: GroupChatRightMsgBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: GroupChatData) {
            item.itemData = groupChatData
        }
    }

    inner class MetaChatViewHolder(val item: GroupChatMetadataBinding) :
        ChatViewHolder(item) {
        override fun bindData(groupChatData: GroupChatData) {
            item.itemData = groupChatData
        }
    }
}

private const val LEFT_TEXT_MESSAGE = 1  //Message is received
private const val RIGHT_TEXT_MESSAGE = 2 //Message is sent
private const val META_DATA_MESSAGE = 3  //Message is of type metadata
private const val ERROR_TEXT_MESSAGE = 0  //Message type is not known