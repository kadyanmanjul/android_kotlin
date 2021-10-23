package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.model.LiveRoomUser
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationUserDiffCallback
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiSpeakersItemBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener

class SpeakerAdapter(
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : RecyclerView.Adapter<SpeakerAdapter.SpeakerViewHolder>() {

    val speakersList: ArrayList<LiveRoomUser> = arrayListOf()

    fun updateFullList(newList: List<LiveRoomUser>) {
        newList.sortedBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateMicViaId(userId:Int,isMicOn:Boolean) {
        val newList: ArrayList<LiveRoomUser> = ArrayList(speakersList)
        val oldUser = newList.filter { it.id == userId }
        newList.removeAll(oldUser)
        oldUser[0]?.isMicOn = isMicOn
        newList.add(oldUser[0])
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun addSingleItem(newItem: LiveRoomUser) {
        val newList: ArrayList<LiveRoomUser> = ArrayList(speakersList)
        newList.add(newItem)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeSingleItem(newItem: LiveRoomUser) {
        val list = ArrayList(speakersList).filter { it.id == newItem.id }
        val newList: ArrayList<LiveRoomUser> = speakersList
        newList.removeAll(list)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeItemIfPresent(newItem: LiveRoomUser) {
        val list = ArrayList(speakersList).filter { it.id == newItem.id }
        val newList: ArrayList<LiveRoomUser> = speakersList
        newList.removeAll(list)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateItem(room: LiveRoomUser, position: Int) {
        speakersList[position] = room
        notifyItemChanged(position)
    }

    private var listenerUserAction: OnUserItemClickListener? = null

    inner class SpeakerViewHolder(val binding: LiSpeakersItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser) {
            with(binding) {
                name.text = model.name
                userImageIv.apply {
                    clipToOutline = true
                    setUserImageRectOrInitials(
                        model.photoUrl, model.name!!, 24, true, 16,
                        textColor = R.color.black,
                        bgColor = R.color.conversation_room_gray
                    )
                }
                if (model.isSpeaking && model.isSpeaker == true && model.isMicOn) {
                    userImage.setBackgroundResource(R.drawable.golden_ring_27dp_border)
                } else {
                    userImage.setBackgroundResource(R.color.white)
                }

                if (false) {
                    raisedHands.visibility = View.VISIBLE
                } else {
                    raisedHands.visibility = View.GONE
                }

                if (model.isModerator) {
                    speakerBadge.visibility = View.VISIBLE
                } else {
                    speakerBadge.visibility = View.GONE
                }
                if (model.isSpeaker == true && !model.isMicOn) {
                    volumeIcon.visibility = View.VISIBLE
                } else {
                    volumeIcon.visibility = View.GONE
                }

                root.setOnSingleClickListener {
                    if (listenerUserAction != null) {
                        listenerUserAction?.onItemClick(
                            model
                        )
                    }
                }

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiSpeakersItemBinding.inflate(inflater, parent, false)

        return SpeakerViewHolder(binding)
    }

    interface OnUserItemClickListener {
        fun onItemClick(user: LiveRoomUser)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        holder.bind(speakersList[position])
    }

    override fun getItemCount(): Int = speakersList.size
}