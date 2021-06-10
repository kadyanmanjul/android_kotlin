package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser
import com.joshtalks.joshskills.core.setImage
import de.hdodenhof.circleimageview.CircleImageView

class RaisedHandsBottomSheetAdapter(rooms: FirestoreRecyclerOptions<LiveRoomUser>) :
    FirestoreRecyclerAdapter<LiveRoomUser, RaisedHandsBottomSheetAdapter.RaisedHandsViewHolder>(
        rooms
    ) {
    var action: RaisedHandsBottomSheetAction? = null

    class RaisedHandsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val raisedHandUsername: TextView = view.findViewById(R.id.raised_hand_user_name)
        val userPhoto: CircleImageView = view.findViewById(R.id.user_photo)
        val plus: ImageView = view.findViewById(R.id.add_to_speaker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaisedHandsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.li_raised_hands_item, parent, false)

        return RaisedHandsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RaisedHandsViewHolder,
        position: Int,
        model: LiveRoomUser
    ) {
        var clickCount = 0
        holder.raisedHandUsername.text = model.name
        holder.userPhoto.setImage(model.photo_url)
        holder.plus.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION && action != null) {
                if (clickCount == 0) {
                    action?.onItemClick(snapshots.getSnapshot(position), position)
                    clickCount ++
                    holder.plus.setImageResource(R.drawable.ic_small_tick)
                }else{
                    holder.plus.setImageResource(R.drawable.ic_small_tick)
                }
            }
        }
    }

    interface RaisedHandsBottomSheetAction{
        fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int)
    }

    fun setOnItemClickListener(bottomSheetAction: RaisedHandsBottomSheetAction) {
        action = bottomSheetAction
    }
}


