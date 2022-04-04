package com.joshtalks.badebhaiya.feed

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.showToast

class FeedActivity : AppCompatActivity(), FeedAdapter.ConversationRoomItemCallback {

    companion object {
        @JvmStatic
        fun getInstance() = FeedActivity()
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    private val binding by lazy<ActivityFeedBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_feed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getRooms()
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
    }

    fun openCreateRoomDialog() {
        CreateRoom.newInstance().also {
            it.show(supportFragmentManager, "createRoom")
            it.addCallback(object : CreateRoom.CreateRoomCallback {
                override fun onRoomCreated(conversationRoomResponse: ConversationRoomResponse) {
                    // TODO: 01/04/2022 - @kadyanmanjul start conversation room here
                    it.dismiss()
                }

                override fun onError(error: String) {
                    showToast(error)
                    it.dismiss()
                }
            })
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        //TODO : 01/04/2022 - @kadyanmanjul join conversation room here
        Log.d("FeedActivity.kt", "YASH => joinRoom: $room")
    }

    override fun setReminder(room: RoomListResponseItem, view: View) {
        Log.d("FeedActivity.kt", "YASH => setReminder: ${room.startTime}")
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val notificationIntent = NotificationHelper.getNotificationIntent(
            this, Notification(
                title = room.topic ?: "Conversation Room Reminder",
                body = room.speakersData?.name ?: "Conversation Room Reminder",
                id = room.startedBy ?: 0,
                userId = room.speakersData?.userId ?: "",
                type = NotificationType.REMINDER
            )
        )
        val pendingIntent =
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 2000, pendingIntent)
            .also {
                room.isScheduled = true
                viewModel.setReminder(
                    ReminderRequest(
                        roomId = room.roomId,
                        userId = User.getInstance().userId,
                        reminderTime = room.startTimeDate.minus(5 * 60 * 1000)
                    )
                )
            }
    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {
        //TODO : 01/04/2022 - @kadyanmanjul join conversation room here
        Log.d("FeedActivity.kt", "YASH => viewRoom: $room")
    }
}
