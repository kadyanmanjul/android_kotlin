package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.LiSpeakersItemBinding


class SpeakerAdapter(
    rooms: FirestoreRecyclerOptions<LiveRoomUser>,
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : FirestoreRecyclerAdapter<LiveRoomUser, SpeakerAdapter.SpeakerViewHolder>(rooms) {

    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")
    private var listenerUserAction: OnUserItemClickListener? = null

    inner class SpeakerViewHolder(val binding: LiSpeakersItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser, bindingAdapterPosition: Int) {
            with(binding) {
                name.text = model.name
                if (!model.photo_url.isNullOrEmpty()) {
                    userImage.apply {
                        clipToOutline = true
                        setImage(model.photo_url ?: "")
                    }
                } else {
                    Glide.with(root.context).load(R.drawable.ic_call_placeholder)
                        .into(userImage)
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

                root.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && listenerUserAction != null) {
                        listenerUserAction?.onItemClick(
                            snapshots.getSnapshot(bindingAdapterPosition),
                            bindingAdapterPosition
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
        holder.bind(model, holder.bindingAdapterPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiSpeakersItemBinding.inflate(inflater, parent, false)

        return SpeakerViewHolder(binding)
    }

    interface OnUserItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }
}