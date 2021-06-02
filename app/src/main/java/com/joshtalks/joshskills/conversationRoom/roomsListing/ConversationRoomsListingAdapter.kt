package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.joshtalks.joshskills.R


class ConversationRoomsListingAdapter(rooms: FirestoreRecyclerOptions<ConversationRoomsListingItem>) :
    FirestoreRecyclerAdapter<ConversationRoomsListingItem, ConversationRoomsListingAdapter.ConversationRoomViewHolder>(rooms) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationRoomViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(
            R.layout.li_conversion_rooms_ilisting_item, parent, false
        )
        return ConversationRoomViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ConversationRoomViewHolder,
        position: Int,
        model: ConversationRoomsListingItem
    ) {
        holder.roomTopic.text = model.topicName
    }

    class ConversationRoomViewHolder(itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        var roomTopic: TextView = itemView.findViewById(R.id.conversation_topic_name)
    }

}
