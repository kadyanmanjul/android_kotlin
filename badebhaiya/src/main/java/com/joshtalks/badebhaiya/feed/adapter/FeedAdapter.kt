package com.joshtalks.badebhaiya.feed.adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.LiRoomEventBinding
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.model.ConversationRoomType
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.feed.model.SpeakerData
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService.Companion.roomQuestionId
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import kotlinx.coroutines.*
import kotlinx.coroutines.NonDisposableHandle.parent
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class FeedAdapter(private val fromProfile: Boolean = false, private val coroutineScope: CoroutineScope? = null) :
    ListAdapter<RoomListResponseItem, FeedAdapter.FeedViewHolder>(DIFF_CALLBACK) {

    companion object DIFF_CALLBACK : DiffUtil.ItemCallback<RoomListResponseItem>() {
        override fun areItemsTheSame(
            oldItem: RoomListResponseItem,
            newItem: RoomListResponseItem
        ): Boolean {
            return oldItem.roomId == newItem.roomId
        }

        override fun areContentsTheSame(
            oldItem: RoomListResponseItem,
            newItem: RoomListResponseItem
        ): Boolean {
            return oldItem == newItem
        }
    }
    var speaker: SpeakerData?=null

    fun addScheduleRoom(newScheduledRoom: RoomListResponseItem) {
        newScheduledRoom.conversationRoomType = ConversationRoomType.SCHEDULED
        val previousList = currentList.toMutableList()
        previousList.add(newScheduledRoom)
        submitList(previousList.toList())
    }

    fun updateScheduleRoomStatusForSpeaker(position: Int) {
        val previousList = currentList.toMutableList()
        previousList[position].conversationRoomType = ConversationRoomType.LIVE
        submitList(previousList.toList())
    }

    var callback: ConversationRoomItemCallback? = null

    inner class FeedViewHolder(private val item: LiRoomEventBinding) :
        RecyclerView.ViewHolder(item.root) {
        @OptIn(InternalCoroutinesApi::class)
        fun onBind(room: RoomListResponseItem) {
            item.roomData = room
            item.adapter = this@FeedAdapter
            item.viewHolder = this
            val name = room.speakersData?.shortName
            val date = Utils.getMessageTime((room.startTime ?: 0L), false, DateTimeStyle.LONG)
            val time = Utils.getMessageTimeInHours(Date(room.startTime ?: 0))
            item.tvCardHeader.text = item.root.context.getString(R.string.room_card_top_title_header, name, date, time)
            item.root.setOnSingleClickListener() {
                    callback?.viewRoom(room, it,false)

            }

            item.root.setOnLongClickListener{
//                if(room.speakersData?.userId == User.getInstance().userId) {
//                    showPopup(room.roomId)
//                }
//                else {
//                    if (room.conversationRoomType==ConversationRoomType.LIVE)
//                    showLeavePopup(room.roomId,roomQuestionId)
//                }
                return@setOnLongClickListener true
            }

            item.callback = callback

            if (fromProfile && SingleDataManager.pendingPilotAction != null && SingleDataManager.pendingPilotAction == PendingPilotEvent.SET_REMINDER && SingleDataManager.pendingPilotEventData!!.roomId == room.roomId){
                coroutineScope?.launch {
                    delay(1000)
                    item.actionButton.performClick()
                }
                SingleDataManager.pendingPilotAction = null
                SingleDataManager.pendingPilotEventData = null
            }
        }
        fun showPopup(roomId: Int) {
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(item.tvCardTopic.context)
            val inflater = LayoutInflater.from(item.tvCardTopic.context)
            val dialogView = inflater.inflate(R.layout.popup_room, null)
            dialogBuilder.setView(dialogView)
            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
            dialogView.findViewById<AppCompatTextView>(R.id.cancel_room).setOnClickListener {
                alertDialog.dismiss()
            }
            dialogView.findViewById<AppCompatTextView>(R.id.delete_room).setOnClickListener{
                showToast("Ended the Room")
                ConvoWebRtcService().endRoom(roomId)
                alertDialog.dismiss()
            }
        }

        fun showLeavePopup(roomId: Int, roomQuestionId: Int?) {
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(item.tvCardTopic.context)
            val inflater = LayoutInflater.from(item.tvCardTopic.context)
            val dialogView = inflater.inflate(R.layout.leave_popup_room, null)
            dialogBuilder.setView(dialogView)
            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
            dialogView.findViewById<AppCompatTextView>(R.id.cancel_room).setOnClickListener {
                alertDialog.dismiss()
            }
            dialogView.findViewById<AppCompatTextView>(R.id.delete_room).setOnClickListener{
                showToast("Left the Room")
                ConvoWebRtcService().leaveRoom(roomId,roomQuestionId)
                alertDialog.dismiss()
            }
        }
    }

    fun setListener(callback: ConversationRoomItemCallback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = DataBindingUtil.inflate<LiRoomEventBinding>(
            LayoutInflater.from(parent.context),
            R.layout.li_room_event,
            parent,
            false
        )
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    interface ConversationRoomItemCallback {
        fun joinRoom(room: RoomListResponseItem, view: View)
        fun playRoom(room: RoomListResponseItem, view: View)
        fun setReminder(room: RoomListResponseItem, view: View)
        //fun deleteReminder(room: RoomListResponseItem,view: View)
        fun viewProfile(profile: String?, deeplink: Boolean, requestDialog: Boolean)
        fun viewRoom(room: RoomListResponseItem, view: View,deeplink: Boolean)
    }
}