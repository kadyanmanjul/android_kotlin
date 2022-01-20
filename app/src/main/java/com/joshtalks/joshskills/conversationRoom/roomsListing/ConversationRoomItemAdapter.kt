package com.joshtalks.joshskills.conversationRoom.roomsListing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.conversationRoom.model.LiveRoomUser
import com.joshtalks.joshskills.databinding.LiConversionRoomsSpeakersBinding

class ConversationRoomItemAdapter :
    RecyclerView.Adapter<ConversationRoomItemAdapter.ConversationRoomSpeakerViewHolder>() {

    val listSpeakers: ArrayList<LiveRoomUser> = arrayListOf()

    fun addItems(newList: List<LiveRoomUser>) {
        if (newList.isEmpty()) {
            return
        }
        val diffCallback = ConversationRoomSpeakerDiffCallback(listSpeakers, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        listSpeakers.clear()
        listSpeakers.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateItem(user: LiveRoomUser, position: Int) {
        listSpeakers[position] = user
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ConversationRoomSpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiConversionRoomsSpeakersBinding.inflate(inflater, parent, false)
        return ConversationRoomSpeakerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationRoomSpeakerViewHolder, position: Int) {
        holder.bind(listSpeakers.get(position))
    }

    class ConversationRoomSpeakerViewHolder(val binding: LiConversionRoomsSpeakersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: LiveRoomUser) {
            with(binding) {
                speaker.text = model.name
                when (model.isSpeaker) {
                    true -> chatIcon.visibility = View.GONE
                    false -> chatIcon.visibility = View.VISIBLE
                }
            }
        }

    }

    override fun getItemCount() = listSpeakers.size
}