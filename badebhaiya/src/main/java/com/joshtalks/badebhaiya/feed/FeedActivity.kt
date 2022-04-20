package com.joshtalks.badebhaiya.feed

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.SearchFragment
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.Notification
import com.joshtalks.badebhaiya.core.NotificationHelper
import com.joshtalks.badebhaiya.core.NotificationType
import com.joshtalks.badebhaiya.core.PermissionUtils
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.ConversationLiveRoomActivity
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class FeedActivity : AppCompatActivity(), FeedAdapter.ConversationRoomItemCallback {

    companion object {
        @JvmStatic
        fun getInstance() = FeedActivity()
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    private lateinit var binding: ActivityFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed)
        viewModel.getRooms()
        viewModel.setIsBadeBhaiyaSpeaker()
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel

        addObserver()
        initView()
        //setOnClickListener()
    }

    fun onSearchPressed()
    {
        supportFragmentManager.findFragmentByTag(SearchFragment::class.java.simpleName)
        supportFragmentManager.beginTransaction()
            .replace(R.id.root_view,SearchFragment(),SearchFragment::class.java.simpleName)
            .commit()
    }

    private fun initView() {
        User.getInstance().apply {
            binding.profileIv.setUserImageOrInitials(profilePicUrl,firstName.toString())
        }

        binding.recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (recyclerView.canScrollVertically(1).not()) {
                    recyclerView.setPadding(resources.getDimension(R.dimen._8sdp).toInt(), 0,
                        resources.getDimension(R.dimen._8sdp).toInt(), binding.bg.height)
                }
            }
        })
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
    }

    fun openCreateRoomDialog() {
        CreateRoom.newInstance().also {
            it.show(supportFragmentManager, "createRoom")
            it.addRoomCallbacks(object : CreateRoom.CreateRoomCallback {
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

                override fun onRoomSchedule() {
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
        takePermissions(room)
    }

    fun takePermissions(room: RoomListResponseItem? = null) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            if (room == null){
                openCreateRoomDialog()
            }
            else viewModel.joinRoom(room)
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            if (room == null){
                                openCreateRoomDialog()
                            }
                            else viewModel.joinRoom(room)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@FeedActivity,
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@FeedActivity)
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    override fun setReminder(room: RoomListResponseItem, view: View) {
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
                    //room.isScheduled = true
                    viewModel.setReminder(
                        ReminderRequest(
                            roomId = room.roomId.toString(),
                            userId = User.getInstance().userId,
                            reminderTime = room.startTimeDate.minus(5 * 60 * 1000)
                        )
                    )
                }
        }

     override fun deleteReminder(room: RoomListResponseItem, view: View) {
        //room.isScheduled=false
        viewModel.deleteReminder(
            DeleteReminderRequest(
                roomId=room.roomId.toString(),
                userId=User.getInstance().userId
            )
        )
    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {
        room.speakersData?.userId?.let {
            ProfileActivity.openProfileActivity(this,it)
        }
    }
}
