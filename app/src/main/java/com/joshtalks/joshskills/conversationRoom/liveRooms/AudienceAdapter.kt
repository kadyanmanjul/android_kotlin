package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.LiAudienceItemBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener


class AudienceAdapter(
    rooms: FirestoreRecyclerOptions<LiveRoomUser>,
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : FirestoreRecyclerAdapter<LiveRoomUser, AudienceAdapter.SpeakerViewHolder>(rooms) {

    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")
    private var listenerUserAction: OnUserItemClickListener? = null

    inner class SpeakerViewHolder(val binding: LiAudienceItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser, uid: String) {
            with(binding) {
                name.text = model.name
                userImage.apply {
                    clipToOutline = true
                    setUserImageRectOrInitials(
                        model.photo_url, model.name, 22, true, 16,
                        textColor = R.color.black,
                        bgColor = R.color.conversation_room_gray
                    )
                }

                if (model.isIs_speaking && model.isIs_speaker && model.isIs_mic_on) {
                    ringIcon.visibility = View.VISIBLE
                } else {
                    ringIcon.visibility = View.GONE
                }

                if (isModerator && model.isIs_hand_raised) {
                    raisedHands.visibility = View.VISIBLE
                } else {
                    raisedHands.visibility = View.GONE
                }

                if (model.isIs_speaker && !model.isIs_mic_on) {
                    volumeIcon.visibility = View.VISIBLE
                } else {
                    volumeIcon.visibility = View.GONE
                }

                root.setOnSingleClickListener {
                    if (listenerUserAction != null) {
                        listenerUserAction?.onItemClick(
                            model, uid.toInt()
                        )
                    }
                }

            }
        }

    }

    @SuppressLint("LogNotTimber")
    override fun onBindViewHolder(
        holder: SpeakerViewHolder,
        position: Int,
        model: LiveRoomUser
    ) {
        holder.bind(model, snapshots.getSnapshot(position).id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiAudienceItemBinding.inflate(inflater, parent, false)

        return SpeakerViewHolder(binding)
    }

    interface OnUserItemClickListener {
        fun onItemClick(user: LiveRoomUser, userUid: Int)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }
}