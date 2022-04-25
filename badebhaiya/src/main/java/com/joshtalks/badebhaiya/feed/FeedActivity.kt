package com.joshtalks.badebhaiya.feed

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.LiveRoomFragment
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.liveroom.OPEN_ROOM
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
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


        const val CHANNEL_NAME = "channel_name"
        const val UID = "uid"
        const val MODERATOR_UID = "moderator_uid"
        const val TOKEN = "TOKEN"
        const val IS_ROOM_CREATED_BY_USER = "is_room_created_by_user"
        const val ROOM_ID = "room_id"
        const val OPEN_FROM_NOTIFICATION = "open_from_notification"
        const val ROOM_QUESTION_ID = "room_question_id"
        const val TOPIC_NAME = "topic_name"


        fun getFeedActivityIntent(
            context: Context,
            channelName: String?,
            uid: Int?,
            token: String?,
            isRoomCreatedByUser: Boolean,
            roomId: Int?,
            roomQuestionId: Int? = null,
            moderatorId: Int? = null,
            topicName: String? = null,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, FeedActivity::class.java).apply {
            Log.d("ABC2", "getIntent() called")
            putExtra(CHANNEL_NAME, channelName)
            putExtra(UID, uid)
            putExtra(MODERATOR_UID, moderatorId)
            putExtra(TOKEN, token)
            putExtra(IS_ROOM_CREATED_BY_USER, isRoomCreatedByUser)
            putExtra(ROOM_ID, roomId)
            putExtra(ROOM_QUESTION_ID, roomQuestionId)
            putExtra(TOPIC_NAME, topicName)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }

        fun getIntentForNotification(
            context: Context,
            roomId: String,
            topicName: String? = null,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, FeedActivity::class.java).apply {

            putExtra(OPEN_FROM_NOTIFICATION, true)
            putExtra(ROOM_ID, roomId.toInt())
            putExtra(TOPIC_NAME, topicName)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }

    }

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    private val liveRoomViewModel by lazy {
        ViewModelProvider(this)[LiveRoomViewModel::class.java]
    }

    private lateinit var binding: ActivityFeedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed)
        checkAndOpenLiveRoom()
        viewModel.getRooms()
        viewModel.setIsBadeBhaiyaSpeaker()
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel

        addObserver()
        initView()
        //setOnClickListener()
    }

    private fun checkAndOpenLiveRoom() {
        if (intent.getBooleanExtra(LiveRoomFragment.OPEN_FROM_NOTIFICATION, false)){
            LiveRoomFragment.launch(
                this,
                StartingLiveRoomProperties(
                    isActivityOpenFromNotification = true,
                    roomId = intent.getIntExtra(LiveRoomFragment.ROOM_ID, 0),
                    channelTopic = intent.getStringExtra(LiveRoomFragment.TOPIC_NAME)?: "",
                    channelName = intent.getStringExtra(LiveRoomFragment.CHANNEL_NAME)?: "",
                    agoraUid = intent.getIntExtra(LiveRoomFragment.UID, 0),
                    moderatorId = intent.getIntExtra(LiveRoomFragment.MODERATOR_UID, 0),
                    token = intent.getStringExtra(LiveRoomFragment.TOKEN) ?: "",
                    roomQuestionId = intent.getIntExtra(LiveRoomFragment.ROOM_QUESTION_ID, 0),
                    isRoomCreatedByUser = intent.getBooleanExtra(LiveRoomFragment.IS_ROOM_CREATED_BY_USER, false)
                )
            )
        }
    }

    fun onSearchPressed() {
        supportFragmentManager.findFragmentByTag(SearchFragment::class.java.simpleName)
        supportFragmentManager.beginTransaction()
            .replace(R.id.root_view, SearchFragment(), SearchFragment::class.java.simpleName)
            .commit()
    }

    private fun initView() {
        User.getInstance().apply {
            binding.profileIv.setUserImageOrInitials(profilePicUrl, firstName.toString())
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (recyclerView.canScrollVertically(1).not()) {
                    recyclerView.setPadding(
                        resources.getDimension(R.dimen._8sdp).toInt(), 0,
                        resources.getDimension(R.dimen._8sdp).toInt(), binding.bg.height
                    )
                }
            }
        })
    }

    private fun addObserver() {
        viewModel.singleLiveEvent.observe(this, androidx.lifecycle.Observer {
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_PROFILE -> {
                    it.data?.let {
                        val userId = it.getString(USER_ID, EMPTY)
                        if (userId.isNullOrBlank().not()) {
                            ProfileActivity.openProfileActivity(this, userId)
                        }
                    }
                }
                OPEN_ROOM -> {
                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room ->
                            val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(room, it.getString(TOPIC)!!)
                            LiveRoomFragment.launch(this, liveRoomProperties)
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
                        val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(this, topic, createdByUser = true)
                        LiveRoomFragment.launch(this@FeedActivity, liveRoomProperties)
                    }
                    it.dismiss()
                }

                override fun onRoomSchedule() {
                    it.dismiss()
                }

                override fun onError(error: String) {
                    it.dismiss()
                }
            })
        }
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        takePermissions(room)
    }

    private fun takePermissions(room: RoomListResponseItem? = null) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            if (room == null) {
                openCreateRoomDialog()
            } else viewModel.joinRoom(room)
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            if (room == null) {
                                openCreateRoomDialog()
                            } else viewModel.joinRoom(room)
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

    fun takePermissionsXml() {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            openCreateRoomDialog()
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            openCreateRoomDialog()
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
                roomId = room.roomId.toString(),
                userId = User.getInstance().userId
            )
        )
    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {
        room.speakersData?.userId?.let {
            ProfileActivity.openProfileActivity(this, it)
        }
    }
}
