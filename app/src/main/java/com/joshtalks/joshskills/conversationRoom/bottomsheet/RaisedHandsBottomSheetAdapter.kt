package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser

class RaisedHandsBottomSheetAdapter(rooms: FirestoreRecyclerOptions<LiveRoomUser>) :
    FirestoreRecyclerAdapter<LiveRoomUser, RaisedHandsBottomSheetAdapter.RaisedHandsViewHolder>(
        rooms
    ) {

    class RaisedHandsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val raisedHandUsername: TextView = view.findViewById(R.id.raised_hand_user_name)
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
        holder.raisedHandUsername.text = model.name
    }
}


