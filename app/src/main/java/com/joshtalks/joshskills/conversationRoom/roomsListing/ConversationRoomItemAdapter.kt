package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.joshtalks.joshskills.R

class ConversationRoomItemAdapter(
    rooms: FirestoreRecyclerOptions<ConversationRoomSpeakerList>
) :
    FirestoreRecyclerAdapter<ConversationRoomSpeakerList, ConversationRoomItemAdapter.ConversationRoomSpeakerViewHolder>(
        rooms
    ) {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConversationRoomSpeakerViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(
            R.layout.li_conversion_rooms_speakers, parent, false
        )
        return ConversationRoomSpeakerViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ConversationRoomSpeakerViewHolder,
        position: Int,
        model: ConversationRoomSpeakerList
    ) {
        holder.speaker.text = model.name
    }

    class ConversationRoomSpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var speaker: TextView = itemView.findViewById(R.id.speaker)
    }
}