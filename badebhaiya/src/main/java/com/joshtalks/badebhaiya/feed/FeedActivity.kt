package com.joshtalks.badebhaiya.feed

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.SearchFragment
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.PermissionUtils
import com.joshtalks.badebhaiya.core.models.FormResponse
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.databinding.WhyRoomBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.*
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.notifications.NotificationScheduler
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.profile.request.DeleteReminderRequest
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.ConversationRoomRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.PeopleToFollowActivity
import com.joshtalks.badebhaiya.signup.fragments.PeopleToFollowFragment
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.setImage
import com.joshtalks.badebhaiya.utils.urlToBitmap
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
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
        const val USER_ID = "user_id"


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

            Log.i("CHECKNOTIFICATION", "getIntentForNotification: $roomId &&& TOPIC:-$topicName")
            Timber.d("INTENT FOR NOTIFICATION DATA => $roomId $topicName")
            putExtra(OPEN_FROM_NOTIFICATION, true)
            putExtra(ROOM_ID, roomId.toInt())
            putExtra(TOPIC_NAME, topicName)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            flags.forEach { flag ->
                this.addFlags(flag)
            }

        }

        fun getIntentForProfile(context: Context, userId: String): Intent {
            return Intent(context, FeedActivity::class.java).also {
                it.putExtra(USER_ID, userId)
            }
        }

    }

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }
    private val profileViewModel by lazy{
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    private var pendingIntent: PendingIntent? = null

    private val liveRoomViewModel by lazy {
        ViewModelProvider(this)[LiveRoomViewModel::class.java]
    }

    private lateinit var binding: ActivityFeedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("sahil", "onCreate of feed activity ")
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, this.theme)
        }
//        var intent=Intent()
//        var bundle=intent.extras
        var user = intent.getStringExtra("userId")
        val mUserId = intent.getStringExtra(USER_ID)

//        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel


        Timber.d("FEED INTENT ${intent.extras}")
        if (user != null) {
            viewProfile(user, true)
        } else if (mUserId != null){
            viewProfile(mUserId, false)
        } else if (SingleDataManager.pendingPilotAction != null) {
            viewProfile(SingleDataManager.pendingPilotEventData!!.pilotUserId, true)
        }
        if (User.getInstance().isLoggedIn()) {
            viewModel.setIsBadeBhaiyaSpeaker()
            addObserver()
            initView()
            PermissionUtils.demandAlarmPermission(this, object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    Timber.d("ALARM PERMISSION ACCEPTED")
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                }

            })
            checkAndOpenLiveRoom()
        }
        //setOnClickListener()
    }

    override fun onRestart() {
        super.onRestart()
        checkAndOpenLiveRoom()

    }

    fun userid(): String {

        return User.getInstance().userId
    }

    override fun onResume() {
        super.onResume()
        if (User.getInstance().isLoggedIn()) {
            viewModel.getRooms()
        }
    }

    private fun checkAndOpenLiveRoom() {
        Timber.d("FEED ACIVITY ON RESTART  => ${intent.extras}")
        Log.i("CHECKNOTIFICATION", "checkAndOpenLiveRoom: ${intent.getIntExtra(ROOM_ID,0)} && topic:-${intent.getStringExtra(TOPIC_NAME)} ----- boolean:- ${intent.getBooleanExtra(OPEN_FROM_NOTIFICATION, false)}")
        if (intent.getBooleanExtra(OPEN_FROM_NOTIFICATION, false)) {

            // TODO: Open Live Room.

            Timber.d(
                "CHECK AND OPEN LIVE ROOM ID => ${
                    intent.getIntExtra(
                        ROOM_ID,
                        0
                    )
                } and topic name => ${intent.getStringExtra(TOPIC_NAME)}"
            )
            Log.i("CHECKNOTIFICATION", "checkAndOpenLiveRoom: ${intent.getIntExtra(ROOM_ID,0)} && topic:-${intent.getStringExtra(TOPIC_NAME)}")
            takePermissions(
                intent.getIntExtra(ROOM_ID, 0).toString(),
                intent.getStringExtra(TOPIC_NAME) ?: ""
            )


//            LiveRoomFragment.launch(
//                this,
//                StartingLiveRoomProperties(
//                    isActivityOpenFromNotification = true,
//                    roomId = intent.getIntExtra(LiveRoomFragment.ROOM_ID, 0),
//                    channelTopic = intent.getStringExtra(LiveRoomFragment.TOPIC_NAME) ?: "",
//                    channelName = intent.getStringExtra(LiveRoomFragment.CHANNEL_NAME) ?: "",
//                    agoraUid = intent.getIntExtra(LiveRoomFragment.UID, 0),
//                    moderatorId = intent.getIntExtra(LiveRoomFragment.MODERATOR_UID, 0),
//                    token = intent.getStringExtra(LiveRoomFragment.TOKEN) ?: "",
//                    roomQuestionId = intent.getIntExtra(LiveRoomFragment.ROOM_QUESTION_ID, 0),
//                    isRoomCreatedByUser = intent.getBooleanExtra(
//                        LiveRoomFragment.IS_ROOM_CREATED_BY_USER,
//                        false
//                    )
//                ),
//                liveRoomViewModel
//            )
        }
    }

    fun onProfileClicked() {
        val fragment = ProfileFragment() // replace your custom fragment class
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_OWN_PROFILE"))

        val bundle = Bundle()
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        bundle.putString("user", User.getInstance().userId) // use as per your need
        bundle.putString("source","FEED_SCREEN")

        fragment.arguments = bundle
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.replace(R.id.root_view, fragment)
        fragmentTransaction.commit()
    }

    fun onSearchPressed() {
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_SEARCH"))

        if (liveRoomViewModel.pubNubState.value != null && liveRoomViewModel.pubNubState.value == PubNubState.STARTED) {
            liveRoomViewModel.liveRoomState.value = LiveRoomState.EXPANDED
        } else {
            supportFragmentManager.findFragmentByTag(SearchFragment::class.java.simpleName)
            supportFragmentManager.beginTransaction()
                .replace(R.id.root_view, SearchFragment(), SearchFragment::class.java.simpleName)
                .commit()
        }

    }

    private fun initView() {
        if(User.getInstance().profilePicUrl!=null) {
            User.getInstance().apply {
                profilePicUrl?.let { binding.profileIv.setImage(it, radius = 16) }
                //binding.profileIv.setUserImageOrInitials(profilePicUrl, firstName.toString())
            }
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
        liveRoomViewModel.pubNubState.observe(this) {
            when (it) {
                PubNubState.STARTED -> onPubNubStart()
                PubNubState.ENDED -> onPubNubEnd()
            }
        }

        viewModel.singleLiveEvent.observe(this, androidx.lifecycle.Observer {
            Log.d("ABC2", "Data class called with feed data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_PROFILE -> {
                    var bundle = Bundle()
                    bundle.putString("user", it.data.getString(USER_ID, EMPTY))
                    supportFragmentManager.findFragmentByTag(ProfileFragment::class.java.simpleName)
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.root_view,
                            ProfileFragment(),
                            ProfileFragment::class.java.simpleName
                        )
                        .commit()
                }
                OPEN_ROOM -> {

                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room ->
                            val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(
                                room,
                                it.getString(TOPIC)!!
                            )
                            LiveRoomFragment.launch(this, liveRoomProperties, liveRoomViewModel, viewModel.source,false)
                        }
                    }
                }
                ROOM_EXPAND->{
                    liveRoomViewModel.liveRoomState.value=LiveRoomState.EXPANDED
                }
                SCROLL_TO_TOP -> {
                    //binding.recyclerView.layoutManager?.scrollToPosition(0)
                    binding.recyclerView.layoutManager?.smoothScrollToPosition(
                        binding.recyclerView,
                        null,
                        0
                    )
                }
            }
        })
    }

    private fun onPubNubEnd() {
        changeToolbarIcon(R.drawable.ic_search)
    }

    private fun onPubNubStart() {
        changeToolbarIcon(R.drawable.ic_baseline_arrow_up)
    }

    private fun changeToolbarIcon(image: Int) {
        binding.search.setImageResource(image)
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
                        val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(
                            this,
                            topic,
                            createdByUser = true
                        )
                        LiveRoomFragment.launch(
                            this@FeedActivity,
                            liveRoomProperties,
                            liveRoomViewModel,
                            "Feed",
                            true
                        )
                    }
                    it.dismiss()
                }

                override fun onRoomSchedule(room: RoomListResponseItem) {
                    notificationScheduler.scheduleNotificationsForSpeaker(this@FeedActivity, room)
                    it.dismiss()
                }

                override fun onError(error: String) {
                    showToast(error)
                    it.dismiss()
                }
            })
        }
    }

    override fun onBackPressed() {
        if (intent.getBooleanExtra("profile_deeplink", false)) {
            finish()
            return
        }
        super.onBackPressed()
    }

    override fun joinRoom(room: RoomListResponseItem, view: View) {
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_JOIN"))
        viewModel.source="Feed"
        takePermissions(room.roomId.toString(), room.topic)
    }

    private fun takePermissions(roomId: String? = null, roomTopic: String? = null) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            if (roomId == null) {
                openCreateRoomDialog()
            } else viewModel.joinRoom(roomId, roomTopic!!,"FEED_SCREEN")
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            if (roomId == null) {
                                openCreateRoomDialog()
                            } else viewModel.joinRoom(roomId, roomTopic!!,"FEED_SCREEN")
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
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_START"))
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
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_SET_REMINDER"))
        showPopup(room.roomId,User.getInstance().userId)
        Timber.d("ROOM KA STARTING TIME => ${room.currentTime}")

        notificationScheduler.scheduleNotificationAsListener(this, room)
        viewModel.setReminder(
            ReminderRequest(
                roomId = room.roomId.toString(),
                userId = User.getInstance().userId,
                reminderTime = room.startTimeDate,
                false
            )
        )
    }

    fun showPopup(roomId: Int, userId: String) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(binding.feedRoot.context)
        val dialogBinding = WhyRoomBinding.inflate(layoutInflater)
//        val dialogView = inflater.inflate(R.layout.why_room, null)
        dialogBuilder.setView(dialogBinding.root)
        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        dialogBinding.message.addTextChangedListener {
            if (it.toString().trim().isEmpty()){
                dialogBinding.Skip.isEnabled = true
                dialogBinding.submit.isEnabled = false
            } else {
                dialogBinding.Skip.isEnabled = false
                dialogBinding.submit.isEnabled = true
            }
        }

        dialogBinding.submit.setOnClickListener{
            val msg:String
            if(dialogBinding.message.toString().isNotBlank()) {
                msg = dialogBinding.message.text.toString()
                val obj= FormResponse(userId,msg,roomId)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resp= CommonRepository().sendMsg(obj)
                        if(resp.isSuccessful)
                            showToast("response Send")

                    }catch (e: Exception){

                    }
                    alertDialog.dismiss()

//                    else
//                        showToast("An Error Occured")
                }

            }
//            else
//                showToast("Please Enter a Message")
        }
        dialogBinding.Skip.setOnClickListener {
            alertDialog.dismiss()
        }

    }

    fun openProfile(profile: String) {
        var bundle = Bundle()
        bundle.putString("user", profile)
        supportFragmentManager.findFragmentByTag(ProfileFragment::class.java.simpleName)
        supportFragmentManager.beginTransaction()
            .replace(R.id.root_view, ProfileFragment(), ProfileFragment::class.java.simpleName)
            .commit()
    }

    override fun viewProfile(profile: String?, deeplink: Boolean) {
        val fragment = ProfileFragment() // replace your custom fragment class

        val bundle = Bundle()
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        bundle.putString("user", profile) // use as per your need
        bundle.putBoolean("deeplink", deeplink)

        fragment.arguments = bundle
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.replace(R.id.root_view, fragment)
        fragmentTransaction.commit()
    }

    override fun viewRoom(room: RoomListResponseItem, view: View) {
//        room.speakersData?.userId?.let {
//            ProfileActivity.openProfileActivity(this, it)
////        }
//        ProfileActivity().apply{
//            room.speakersData?.userId?.let {
//                arguments=Bundle().apply {putString("user",it)}
//            }
//
//        }
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_CARD"))
        val fragment = ProfileFragment() // replace your custom fragment class

        val bundle = Bundle()
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        bundle.putString("user", room.speakersData?.userId) // use as per your need
        bundle.putString("source","FEED_SCREEN")

        fragment.arguments = bundle
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.replace(R.id.root_view, fragment)
        fragmentTransaction.commit()

//        supportFragmentManager.findFragmentByTag(ProfileActivity::class.java.simpleName)
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.root_view, ProfileActivity(), ProfileActivity::class.java.simpleName)
//            .commit()
    }
}
