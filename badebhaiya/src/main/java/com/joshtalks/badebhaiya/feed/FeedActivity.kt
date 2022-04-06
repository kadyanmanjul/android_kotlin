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
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.ConversationLiveRoomActivity
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.Utils

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
        addObserver()
    }

    private fun addObserver() {
        viewModel.singleLiveEvent.observe(this, androidx.lifecycle.Observer {
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_PROFILE ->{
                    it.data?.let {
                        val userId = it.getString(USER_ID, EMPTY)
                        if (userId.isNullOrBlank().not()){
                            ProfileActivity.openProfileActivity(this,userId)
                        }
                    }
                }
                OPEN_ROOM ->{
                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room->
                                ConversationLiveRoomActivity.startRoomActivity(
                                    activity = this@FeedActivity,
                                    channelName = room.channelName,
                                    uid = room.uid,
                                    token =room.token,
                                    isRoomCreatedByUser = room.moderatorId == room.uid,
                                    roomId = room.roomId,
                                    moderatorId = room.moderatorId,
                                    topicName = it.getString(TOPIC),
                                    flags = arrayOf()

                                )
                        }
                    }
                }
            }
        })
        if(User.getInstance().profilePicUrl.isNullOrEmpty().not())
            Utils.setImage(binding.profileIv,User.getInstance().profilePicUrl.toString())
    }

    fun openCreateRoomDialog() {
        CreateRoom.newInstance().also {
            it.show(supportFragmentManager, "createRoom")
            it.addCallback(object : CreateRoom.CreateRoomCallback {
                override fun onRoomCreated(
                    conversationRoomResponse: ConversationRoomResponse,
                    topic: String
                ) {
                    conversationRoomResponse.apply {
                        ConversationLiveRoomActivity.startRoomActivity(
                            activity = this@FeedActivity,
                            channelName = this.channelName,
                            uid = this.uid,
                            token =this.token,
                            isRoomCreatedByUser = true ,
                            roomId = this.roomId,
                            moderatorId = this.moderatorId,
                            topicName = topic,
                            flags = arrayOf()

                        )
                    }
                    it.dismiss()
                }

                override fun onError(error: String) {
                    showToast(error)
                    Log.d("Manjul", "onError() called with: error = $error")
                    it.dismiss()
                }
            })
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        //TODO : 01/04/2022 - @kadyanmanjul join conversation room here
        Log.d("Manjul", "joinRoom() called with: room = $room, view = $view")
        viewModel.joinRoom(room)
    }

    override fun setReminder(room: RoomListResponseItem, view: View) {
        Log.d("Manjul", "setReminder() called with: room = $room, view = $view")
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
                        roomId = room.roomId.toString(),
                        userId = User.getInstance().userId,
                        reminderTime = room.startTimeDate.minus(5 * 60 * 1000)
                    )
                )
            }
    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {
        //TODO : 01/04/2022 - @kadyanmanjul join conversation room here
        Log.d("Manjul", "viewRoom() called with: roomid = ${room.speakersData?.userId}, room = $room")
        room.speakersData?.userId?.let {
            ProfileActivity.openProfileActivity(this,it)
        }
    }
}
