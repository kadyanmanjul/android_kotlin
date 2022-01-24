package com.joshtalks.joshskills.ui.lesson.conversationRoom.liveRooms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DEFAULT_NAME
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiAudienceItemBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.lesson.conversationRoom.model.LiveRoomUser
import com.joshtalks.joshskills.ui.lesson.conversationRoom.roomsListing.ConversationUserDiffCallback

class AudienceAdapter(
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : RecyclerView.Adapter<AudienceAdapter.SpeakerViewHolder>() {

    val audienceList: ArrayList<LiveRoomUser> = arrayListOf()
    private var listenerUserAction: OnUserItemClickListener? = null

    fun updateFullList(newList: List<LiveRoomUser>) {
        newList.sortedByDescending { it.sortOrder }
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
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