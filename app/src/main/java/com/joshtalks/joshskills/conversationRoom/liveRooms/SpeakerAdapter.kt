package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setImage
import de.hdodenhof.circleimageview.CircleImageView


class SpeakerAdapter(
    rooms: FirestoreRecyclerOptions<LiveRoomUser>,
    val action: ConversationLiveRoomSpeakerClickAction,
    val isModerator: Boolean
) : FirestoreRecyclerAdapter<LiveRoomUser, SpeakerAdapter.SpeakerViewHolder>(rooms) {

    val firebaseFirestore = FirebaseFirestore.getInstance().collection("conversation_rooms")
    private val TAG = "SpeakerAdapter"
    private var listenerUserAction: OnUserItemClickListener? = null

    class SpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val photo: CircleImageView = itemView.findViewById(R.id.user_image)
        val handRaisedIcon: ImageView = itemView.findViewById(R.id.raised_hands)
        val micIcon: ImageView = itemView.findViewById(R.id.volume_icon)

    }

    @SuppressLint("LogNotTimber")
    override fun onBindViewHolder(
        holder: SpeakerViewHolder,
        position: Int,
        model: LiveRoomUser
    ) {
        val documentSnapshot = snapshots.getSnapshot(position)
        val id = documentSnapshot.id


        holder.name.text = model.name
        if (!model.photo_url.isNullOrEmpty()) {
            holder.photo.setImage(model.photo_url ?: "")
        } else {
            Glide.with(holder.itemView.context).load(R.drawable.ic_call_placeholder)
                .into(holder.photo)
        }

        if (isModerator && model.isIs_hand_raised) {
            holder.handRaisedIcon.visibility = View.VISIBLE
            Log.d(TAG, "onBindViewHolder: Hand Raised ")
        } else {
            holder.handRaisedIcon.visibility = View.GONE
            Log.d(TAG, "onBindViewHolder: Hand UnRaised ")

        }

        if (model.isIs_speaker && !model.isIs_mic_on) {
            holder.micIcon.visibility = View.VISIBLE
            Log.d(TAG, "onBindViewHolder: Mic off ${model.isIs_mic_on} ")
        } else {
            holder.micIcon.visibility = View.GONE
            Log.d(TAG, "onBindViewHolder: Mic on ")
            Log.d(TAG, "onBindViewHolder: Mic off ${model.isIs_mic_on} ")

        }

        holder.itemView.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && listenerUserAction != null) {
                listenerUserAction?.onItemClick(snapshots.getSnapshot(position), position);
            }
        }

        Log.d("SpeakerAdapter", "position: $position , ${model.name}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.li_speakers_item, parent, false)

        return SpeakerViewHolder(view)
    }

    interface OnUserItemClickListener {
        fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int)
    }

    fun setOnItemClickListener(listenerUser: OnUserItemClickListener) {
        listenerUserAction = listenerUser
    }
}