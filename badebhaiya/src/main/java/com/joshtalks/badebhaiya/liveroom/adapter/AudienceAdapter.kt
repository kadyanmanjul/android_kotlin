package com.joshtalks.badebhaiya.liveroom.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.databinding.LiAudienceItemBinding
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import timber.log.Timber

class AudienceAdapter(
    val isModerator: Boolean
) : ListAdapter<LiveRoomUser, AudienceAdapter.SpeakerViewHolder>(AudienceDiffUtil()) {

    init {
        Timber.tag("localuser").d("AUDIENCE ADAPTER INIT CALLED")
    }
    private var listenerUserAction: OnUserItemClickListener? = null
    val handRaisedList: ArrayList<LiveRoomUser> = arrayListOf()
    fun updateFullList(newList: List<LiveRoomUser>) {
        Timber.d("AUDIENCE LIST IN ADAPTER => $newList")
        val diffCallback = ConversationUserDiffCallback(handRaisedList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        handRaisedList.clear()
        handRaisedList.addAll(newList)
        Timber.d("AUDEN")
        Timber.tag("LiveRoomAudience").d("AUDIENCE LIST IN AFTER AFTER DOING DIFFUTIL CALCULATION => $newList")
        diffResult.dispatchUpdatesTo(this)
    }


    inner class SpeakerViewHolder(val binding: LiAudienceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser) {
            Timber.tag("localuser")
                .d("RECYCLERVIEW BIND CALLED AND LIST IS $currentList AND ITEM IS => $model")

            with(binding) {
                name.text = model.name
                userImage.apply {
                    clipToOutline = true
                    setUserImageRectOrInitials(
                        model.photoUrl,
                        model.name ?: DEFAULT_NAME,
                        22,
                        true,
                        16,
                        textColor = R.color.black,
                        bgColor = R.color.conversation_room_gray
                    )
                }
                if(handRaisedList.contains(model))
                    raisedHands.visibility=View.VISIBLE
                if (isModerator && model.isHandRaised) {
                    raisedHands.visibility = View.VISIBLE
                } else {
                    raisedHands.visibility = View.GONE
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
        val binding = LiAudienceItemBinding.inflate(inflater, parent, false)
        Timber.tag("localuser").d("RECYCLERVIEW ITEM INFLATED")
        return SpeakerViewHolder(binding)
    }

    interface OnUserItemClickListener {
        fun onItemClick(user: LiveRoomUser)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class AudienceDiffUtil : DiffUtil.ItemCallback<LiveRoomUser>() {
    override fun areItemsTheSame(oldItem: LiveRoomUser, newItem: LiveRoomUser): Boolean {
        val itemSame = oldItem.id == newItem.id
        Timber.d("IS ITEM SAME => $itemSame")
        return itemSame
    }

    override fun areContentsTheSame(oldItem: LiveRoomUser, newItem: LiveRoomUser): Boolean {
        val contentSame = oldItem.id == newItem.id &&
                oldItem.userId == newItem.userId &&
                oldItem.isSpeaker == newItem.isSpeaker &&
                oldItem.name == newItem.name &&
                oldItem.fullName == newItem.fullName &&
                oldItem.photoUrl == newItem.photoUrl &&
                oldItem.isModerator == newItem.isModerator &&
                oldItem.isMicOn == newItem.isMicOn &&
                oldItem.isSpeaking == newItem.isSpeaking &&
                oldItem.isHandRaised == newItem.isHandRaised &&
                oldItem.isInviteSent == newItem.isInviteSent &&
                oldItem.isSpeakerAccepted == newItem.isSpeakerAccepted

        Timber.d("IS CONTENT SAME => $contentSame")
        return contentSame
    }
}