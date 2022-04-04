package com.joshtalks.badebhaiya.liveroom.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.databinding.LiSpeakersItemBinding
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials

class SpeakerAdapter : RecyclerView.Adapter<SpeakerAdapter.SpeakerViewHolder>() {

    val speakersList: ArrayList<LiveRoomUser> = arrayListOf()

    fun updateFullList(newList: List<LiveRoomUser>) {
        newList.sortedByDescending { it.sortOrder }
        newList.forEach {
            Log.d("ABC3", "order called ${it.name}  ${it.sortOrder} ")
        }
        Log.d("ABC3", "updateFullList() called with:")
        val diffCallback = ConversationUserDiffCallback(speakersList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        speakersList.clear()
        speakersList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    private var listenerUserAction: OnUserItemClickListener? = null

    inner class SpeakerViewHolder(val binding: LiSpeakersItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        private var userImage: FrameLayout? =null
        var model: LiveRoomUser? =null

        fun bind(model: LiveRoomUser) {
            this.userImage = binding.userImage
            this.model = model
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
        fun setGoldenRingVisibility(isVisible:Boolean = false){
            if (model?.isMicOn == true ) {
                userImage?.let { ring ->
                    if (isVisible) {
                        ring.setBackgroundResource(R.drawable.golden_ring_27dp_border)
                    } else {
                        ring.setBackgroundResource(R.color.white)
                    }
                }
            }
        }

    }

    override fun getItemId(position: Int): Long {
        return speakersList[position].id?.toLong() ?: 0L
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