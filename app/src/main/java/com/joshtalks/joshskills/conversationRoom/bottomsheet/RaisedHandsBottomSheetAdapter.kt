package com.joshtalks.joshskills.conversationRoom.bottomsheet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.LiveRoomUser
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.LiRaisedHandsItemBinding

class RaisedHandsBottomSheetAdapter(rooms: FirestoreRecyclerOptions<LiveRoomUser>) :
    FirestoreRecyclerAdapter<LiveRoomUser, RaisedHandsBottomSheetAdapter.RaisedHandsViewHolder>(
        rooms
    ) {
    var action: RaisedHandsBottomSheetAction? = null

    inner class RaisedHandsViewHolder(val binding: LiRaisedHandsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var clickCount = 0

        fun bind(model: LiveRoomUser, bindingAdapterPosition: Int) {
            with(binding) {
                raisedHandUserName.text = model.name
                if (!model.photo_url.isNullOrEmpty())
                    userPhoto.setImage(model.photo_url)
                addToSpeaker.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && action != null) {
                        if (clickCount == 0) {
                            action?.onItemClick(
                                snapshots.getSnapshot(bindingAdapterPosition),
                                bindingAdapterPosition
                            )
                            clickCount++
                            addToSpeaker.setImageResource(R.drawable.ic_selected_user)
                        } else {
                            addToSpeaker.setImageResource(R.drawable.ic_selected_user)
                        }
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaisedHandsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiRaisedHandsItemBinding.inflate(inflater, parent, false)

        return RaisedHandsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RaisedHandsViewHolder,
        position: Int,
        model: LiveRoomUser
    ) {
        holder.bind(model, holder.bindingAdapterPosition)

    }

    interface RaisedHandsBottomSheetAction {
        fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int)
    }

    fun setOnItemClickListener(bottomSheetAction: RaisedHandsBottomSheetAction) {
        action = bottomSheetAction
    }
}


