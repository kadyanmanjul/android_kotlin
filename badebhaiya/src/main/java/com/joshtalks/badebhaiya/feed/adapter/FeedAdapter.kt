package com.joshtalks.badebhaiya.feed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.LiRoomEventBinding
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.feed.model.SpeakerData
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import java.util.Date

class FeedAdapter :
    ListAdapter<RoomListResponseItem, FeedAdapter.FeedViewHolder>(DIFF_CALLBACK) {

    companion object DIFF_CALLBACK : DiffUtil.ItemCallback<RoomListResponseItem>() {
        override fun areItemsTheSame(
            oldItem: RoomListResponseItem,
            newItem: RoomListResponseItem
        ): Boolean {
            return oldItem.roomId == newItem.roomId
        }

        override fun areContentsTheSame(
            oldItem: RoomListResponseItem,
            newItem: RoomListResponseItem
        ): Boolean {
            return oldItem == newItem
        }
    }
    var speaker: SpeakerData?=null

    fun addScheduleRoom(newScheduledRoom: RoomListResponseItem) {
        newScheduledRoom.conversationRoomType = ConversationRoomType.NOT_SCHEDULED
        val previousList = currentList.toMutableList()
        previousList.add(newScheduledRoom)
        submitList(previousList.toList())
    }

    var callback: ConversationRoomItemCallback? = null

    inner class FeedViewHolder(private val item: LiRoomEventBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(room: RoomListResponseItem) {
            item.roomData = room
            val name = room.speakersData?.shortName
            val date = Utils.getMessageTime((room.startTime ?: 0L) * 1000L, false, DateTimeStyle.LONG)
            val time = Utils.getMessageTimeInHours(Date((room.startTime ?: 0) * 1000L))
            item.tvCardHeader.text = item.root.context.getString(R.string.room_card_top_title_header, name, date, time)
            item.root.setOnClickListener {
                if (room.conversationRoomType == ConversationRoomType.LIVE) {
                    callback?.joinRoom(room, it)
                } else  {
                    callback?.viewRoom(room, it)
                }
            }
            item.callback = callback
        }
    }

    fun setListener(callback: ConversationRoomItemCallback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = DataBindingUtil.inflate<LiRoomEventBinding>(
            LayoutInflater.from(parent.context),
            R.layout.li_room_event,
            parent,
            false
        )
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    interface ConversationRoomItemCallback {
        fun joinRoom(room: RoomListResponseItem, view: View)
        fun setReminder(room: RoomListResponseItem, view: View)
        fun deleteReminder(room: RoomListResponseItem,view: View)
        fun viewRoom(room: RoomListResponseItem, view: View)
    }
}