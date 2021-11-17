package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.model.LiveRoomUser
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationUserDiffCallback
import com.joshtalks.joshskills.core.DEFAULT_NAME
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiAudienceItemBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener

class AudienceAdapter(
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : RecyclerView.Adapter<AudienceAdapter.SpeakerViewHolder>() {

    val audienceList: ArrayList<LiveRoomUser> = arrayListOf()
    private var listenerUserAction: OnUserItemClickListener? = null

    fun updateHandRaisedViaId(userId:Int,isHandRaised:Boolean) {
        val newList: ArrayList<LiveRoomUser> = ArrayList(audienceList)
        val isOldUserPresent = newList.any { it.id == userId }
        if (isOldUserPresent){
            val oldUser = newList.filter { it.id == userId }
            newList.removeAll(oldUser)
            oldUser[0].isHandRaised = isHandRaised
            newList.add(oldUser[0])
        }
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateFullList(newList: List<LiveRoomUser>) {
        newList.sortedBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        Log.d("ABC2", "updateFullList() called with: audienceList = $audienceList")
        diffResult.dispatchUpdatesTo(this)
    }

    fun addSingleItem(newItem: LiveRoomUser) {
        val newList: ArrayList<LiveRoomUser> = ArrayList(audienceList)
        newList.add(newItem)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        Log.d("ABC2", "addSingleItem() called with: audienceList = $audienceList")
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeSingleItem(newItem: LiveRoomUser) {
        val list = ArrayList(audienceList).filter { it.id == newItem.id }
        val newList: ArrayList<LiveRoomUser> = ArrayList(audienceList)
        newList.removeAll(list)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        Log.d("ABC2", "removeSingleItem() called with: audienceList = $audienceList")
        diffResult.dispatchUpdatesTo(this)
    }
    fun removeItemIfPresent(newItem: LiveRoomUser) {
        Log.d("ABC2", "AremoveItemIfPresent() called with: newItem = $newItem audienceList = $audienceList")
        val list = ArrayList(audienceList).filter { it.id == newItem.id }
        val newList: ArrayList<LiveRoomUser> = ArrayList(audienceList)
        newList.removeAll(list)
        newList.sortBy { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        Log.d("ABC2", "AremoveItemIfPresent() called with: newItem = $newItem audienceList = $audienceList")
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateItem(room: LiveRoomUser, position: Int) {
        audienceList[position] = room
        Log.d("ABC2", "updateItem() called with: room = $room, audienceList = $audienceList")
        notifyItemChanged(position)
    }

    inner class SpeakerViewHolder(val binding: LiAudienceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser) {
            with(binding) {
                name.text = model.name
                userImage.apply {
                    clipToOutline = true
                    setUserImageRectOrInitials(
                        model.photoUrl,
                        model.name?: DEFAULT_NAME,
                        22,
                        true,
                        16,
                        textColor = R.color.black,
                        bgColor = R.color.conversation_room_gray
                    )
                }

                if (isModerator && model.isHandRaised) {
                    raisedHands.visibility = View.VISIBLE
                } else {
                    raisedHands.visibility = View.GONE
                }

                if (model.isSpeaker== true && !model.isMicOn) {
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
        val binding = LiAudienceItemBinding.inflate(inflater, parent, false)

        return SpeakerViewHolder(binding)
    }

    interface OnUserItemClickListener {
        fun onItemClick(user: LiveRoomUser)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        holder.bind(audienceList[position])
    }

    override fun getItemCount(): Int {
        return  audienceList.size
    }
}