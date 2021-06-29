package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser
import com.joshtalks.joshskills.databinding.LiConversionRoomsSpeakersBinding

class ConversationRoomItemAdapter(
    rooms: FirestoreRecyclerOptions<LiveRoomUser>
) :
    FirestoreRecyclerAdapter<LiveRoomUser, ConversationRoomItemAdapter.ConversationRoomSpeakerViewHolder>(
        rooms
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConversationRoomSpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiConversionRoomsSpeakersBinding.inflate(inflater, parent, false)
        return ConversationRoomSpeakerViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ConversationRoomSpeakerViewHolder,
        position: Int,
        model: LiveRoomUser
    ) {
        holder.bind(model)
    }

    class ConversationRoomSpeakerViewHolder(val binding: LiConversionRoomsSpeakersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: LiveRoomUser) {
            with(binding) {
                speaker.text = model.name
                when (model.isIs_speaker) {
                    true -> chatIcon.visibility = View.GONE
                    false -> chatIcon.visibility = View.VISIBLE
                }
            }
        }

    }
}