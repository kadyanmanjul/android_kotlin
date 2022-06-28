package com.joshtalks.badebhaiya.liveroom.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
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
) : RecyclerView.Adapter<AudienceAdapter.SpeakerViewHolder>() {

    val audienceList: ArrayList<LiveRoomUser> = arrayListOf()
    private var listenerUserAction: OnUserItemClickListener? = null

    fun updateFullList(newList: List<LiveRoomUser>) {
        Timber.d("AUDIENCE LIST IN ADAPTER => $newList")
        var fullName= User.getInstance().firstName+" "+ User.getInstance().lastName
        var me= LiveRoomUser(123, // TODO: Add real id
            false,
            User.getInstance().firstName,
            fullName,
            User.getInstance().profilePicUrl,
            sortOrder = null,
            false,
            false,
            false,
            false,
            User.getInstance().userId,
        )
//        newList.sortedByDescending { it.sortOrder }
        Timber.tag("LiveRoomAudience").d("AUDIENCE LIST IN AFTER ADDING LOCAL USER => $newList")
        val diffCallback = ConversationUserDiffCallback(audienceList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        audienceList.clear()
        audienceList.addAll(newList)
        audienceList.remove(me)
        Timber.d("AUDEN")
        Timber.tag("LiveRoomAudience").d("AUDIENCE LIST IN AFTER AFTER DOING DIFFUTIL CALCULATION => $newList")
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
        Timber.tag("LiveRoomAudience").d("AUDIENCE LIST IN AFTER COMING INTO ADAPTER => $audienceList and Size => ${audienceList.size}")
        return  audienceList.size
    }
}