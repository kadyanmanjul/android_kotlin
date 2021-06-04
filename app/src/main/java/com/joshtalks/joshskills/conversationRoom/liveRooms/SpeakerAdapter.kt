package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setImage
import de.hdodenhof.circleimageview.CircleImageView

class SpeakerAdapter(
    rooms: FirestoreRecyclerOptions<ConversationLiveRoomUser>,
    val action: ConversationLiveRoomSpeakerClickAction,
    val isSpeakerList: Boolean
) : FirestoreRecyclerAdapter<ConversationLiveRoomUser, SpeakerAdapter.SpeakerViewHolder>(rooms) {

    class SpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
        val photo: CircleImageView = itemView.findViewById(R.id.user_image)

    }

    @SuppressLint("LogNotTimber")
    override fun onBindViewHolder(
        holder: SpeakerViewHolder,
        position: Int,
        model: ConversationLiveRoomUser
    ) {
        val documentSnapshot = snapshots.getSnapshot(position)
        val id = documentSnapshot.id

        holder.name.text = model.name
        if (!model.photo_url.isNullOrEmpty()) {
            holder.photo.setImage(model.photo_url ?: "")
        }
        Log.d("SpeakerAdapter", "position: $position , ${model.name}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.li_speakers_item, parent, false)

        return SpeakerViewHolder(view)
    }
}