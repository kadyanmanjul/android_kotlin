package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.databinding.LiRaisedHandsItemBinding
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.liveroom.adapter.ConversationUserDiffCallback
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials

class RaisedHandsBottomSheetAdapter() :
 RecyclerView.Adapter<RaisedHandsBottomSheetAdapter.RaisedHandsViewHolder>() {

    var action: RaisedHandsBottomSheetAction? = null
    val handRaisedList: ArrayList<LiveRoomUser> = arrayListOf()

    fun updateFullList(newList: List<LiveRoomUser>?) {
        if (newList == null || newList.isEmpty()){
            handRaisedList.clear()
            notifyDataSetChanged()
            return
        }
        newList.sortedBy { it.sortOrder }
        Log.d("ABC", "updateFullList() called with: newList = $newList")
        val diffCallback = ConversationUserDiffCallback(handRaisedList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback,true)
        handRaisedList.clear()
        handRaisedList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
    inner class RaisedHandsViewHolder(val binding: LiRaisedHandsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: LiveRoomUser, position: Int) {
            with(binding) {
                raisedHandUserName.text = model.name
                userPhoto.apply {
                    setUserImageRectOrInitials(
                        model.photoUrl, model.name?: DEFAULT_NAME, 22, true, 16,
                        textColor = R.color.black,
                        bgColor = R.color.conversation_room_gray
                    )
                }
                addToSpeaker.setOnSingleClickListener {
                    if (position != RecyclerView.NO_POSITION && !model.isInviteSent) {
                        action?.onItemClick(model, position)
                        addToSpeaker.setImageResource(R.drawable.ic_selected_user)
                    }
                }

                when (model.isInviteSent) {
                    true -> addToSpeaker.setImageResource(R.drawable.ic_selected_user)
                    false -> addToSpeaker.setImageResource(R.drawable.ic_unselected_user)

                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaisedHandsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiRaisedHandsItemBinding.inflate(inflater, parent, false)

        return RaisedHandsViewHolder(binding)
    }

    interface RaisedHandsBottomSheetAction {
        fun onItemClick(liveRoomUser: LiveRoomUser, position: Int)
    }

    fun setOnItemClickListener(bottomSheetAction: RaisedHandsBottomSheetAction) {
        action = bottomSheetAction
    }

    override fun onBindViewHolder(holder: RaisedHandsViewHolder, position: Int) {
        holder.bind(handRaisedList[position], position)
    }

    override fun getItemCount(): Int {
        return handRaisedList.size
    }
}


