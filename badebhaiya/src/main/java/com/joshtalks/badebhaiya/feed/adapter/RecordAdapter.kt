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
import com.joshtalks.badebhaiya.databinding.LiRecordRoomEventBinding
import com.joshtalks.badebhaiya.databinding.LiRoomEventBinding
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.model.*
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

class RecordAdapter(private val fromProfile: Boolean = false, private val coroutineScope: CoroutineScope? = null) :
    ListAdapter<RecordedResponse, RecordAdapter.FeedViewHolder>(DIFF_CALLBACK) {

    companion object DIFF_CALLBACK : DiffUtil.ItemCallback<RecordedResponse>() {
        override fun areItemsTheSame(
            oldItem: RecordedResponse,
            newItem: RecordedResponse
        ): Boolean {
            return oldItem.recordList.roomId == newItem.recordList.roomId
        }

        override fun areContentsTheSame(
            oldItem: RecordedResponse,
            newItem: RecordedResponse
        ): Boolean {
            return oldItem == newItem
        }
    }
    var speaker: SpeakerData?=null

    fun addScheduleRoom(newScheduledRoom: RecordedResponse) {
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

    var callback: RecordedRoomItemCallback? = null

    inner class FeedViewHolder(private val item: LiRecordRoomEventBinding) :
        RecyclerView.ViewHolder(item.root) {
        @OptIn(InternalCoroutinesApi::class)
        fun onBind(room: RecordedRoomItem) {
            item.roomData = room
            item.adapter = this@RecordAdapter
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

    fun setListener(callback: RecordedRoomItemCallback) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = DataBindingUtil.inflate<LiRecordRoomEventBinding>(
            LayoutInflater.from(parent.context),
            R.layout.li_room_event,
            parent,
            false
        )
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.onBind(getItem(position).recordList)
    }

    interface RecordedRoomItemCallback {
        fun joinRoom(room: RoomListResponseItem, view: View)
        fun setReminder(room: RoomListResponseItem, view: View)
        //fun deleteReminder(room: RoomListResponseItem,view: View)
        fun viewProfile(profile: String?, deeplink:Boolean)
        fun viewRoom(room: RecordedRoomItem, view: View,deeplink: Boolean)
    }
}