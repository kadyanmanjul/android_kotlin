package com.joshtalks.badebhaiya.feed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.LiRoomEventBinding
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem

class FeedAdapter :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    var itemClick: ((RoomListResponseItem, View) -> Unit)? = null
    var roomList: List<RoomListResponseItem> = listOf()

    fun updateRoomList(roomList: List<RoomListResponseItem>){
        this.roomList = roomList
    }
    inner class FeedViewHolder(private val item: LiRoomEventBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(room: RoomListResponseItem) {
            item.roomData = room
            item.root.setOnClickListener {
                itemClick?.invoke(room, item.root)
            }
        }
    }

    fun setListener(function: ((RoomListResponseItem, View) -> Unit)?) {
        itemClick = function
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
        holder.onBind(roomList[position])
    }

    override fun getItemCount() = roomList.size
}