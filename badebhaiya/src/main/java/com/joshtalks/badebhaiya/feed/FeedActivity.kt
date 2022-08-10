package com.joshtalks.badebhaiya.feed

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.transition.Fade
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.SearchFragment
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.models.FormResponse
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.databinding.WhyRoomBinding
import com.joshtalks.badebhaiya.feed.adapter.FeedAdapter
import com.joshtalks.badebhaiya.feed.joinPreviousRoom.PreviousRoomDialog
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.*
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.mediaPlayer.RecordedRoomFragment
import com.joshtalks.badebhaiya.notifications.NotificationScheduler
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.profile.request.ReminderRequest
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.showCallRequests.RequestBottomSheetFragment
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.setImage
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
class FeedActivity : AppCompatActivity(), FeedAdapter.ConversationRoomItemCallback, CreateRoom.CreateRoomCallback {

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
        const val ROOM_REQUEST_ID = "room_request_id"


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

        fun getIntentForRoomRequest(context: Context, userId: String): Intent {
            return Intent(context, FeedActivity::class.java).also {
                it.putExtra(ROOM_REQUEST_ID, userId)
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

    private val liveRoomViewModel by lazy {
        ViewModelProvider(this)[LiveRoomViewModel::class.java]
    }

    private val badgeDrawable: BadgeDrawable by lazy { BadgeDrawable.create(this) }

    private lateinit var binding: ActivityFeedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("sahil", "onCreate of feed activity ")
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, this.theme)
        }

        var user = intent.getStringExtra("userId")
        var requestDialog=intent.getBooleanExtra("request_dialog",false)
        val mUserId = intent.getStringExtra(USER_ID)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        binding.user = User.getInstance()


        Timber.d("FEED INTENT ${intent.extras}")

        val roomRequestId = intent.getStringExtra(ROOM_REQUEST_ID)

        viewModel.roomRequestCount.value=0
         if(!roomRequestId.isNullOrEmpty()){
            RequestBottomSheetFragment.open(roomRequestId, supportFragmentManager)

        } else if (user != null) {
            viewProfile(user, true,requestDialog)
        } else if (mUserId != null){
            viewProfile(mUserId, false,requestDialog)
        } else if (SingleDataManager.pendingPilotAction != null) {
            viewProfile(SingleDataManager.pendingPilotEventData!!.pilotUserId, true, requestDialog)
        }
        requestDialog=false
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


    }

    override fun onRestart() {
        super.onRestart()
        checkAndOpenLiveRoom()
//        viewModel.getRoomRequestCount()

    }

    fun userid(): String {

        return User.getInstance().userId
    }

    override fun onResume() {
        super.onResume()
//        viewModel.getRoomRequestCount()
        if (User.getInstance().isLoggedIn()) {
            viewModel.getRecordRooms()
        }
        if (PubNubManager.isRoomActive){
            try {
                ConvoWebRtcService.rtcEngine?.let {
                    Log.d("ABCService", "AUDIO IS INCREASED")
                    it.setDefaultAudioRoutetoSpeakerphone(true)
                    it.enableAudioVolumeIndication(1800, 3, true)
                    it.adjustRecordingSignalVolume(400)

                }
            } catch (e: Exception){

            }
        }
    }

    private fun checkAndOpenLiveRoom() {
        Timber.d("FEED ACIVITY ON RESTART  => ${intent.extras}")
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
            takePermissions(
                intent.getIntExtra(ROOM_ID, 0).toString(),
                intent.getStringExtra(TOPIC_NAME) ?: "",
                "moderatorId"
            )
        }
    }

    fun onProfileClicked() {
        val fragment = ProfileFragment() // replace your custom fragment class
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_OWN_PROFILE"))

        val bundle = Bundle()
        fragment?.apply {
            exitTransition = MaterialSharedAxis(
                MaterialSharedAxis.Z,
                /* forward= */ false
            ).apply {
                duration = 500
            }
        }
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        bundle.putString("user", User.getInstance().userId) // use as per your need
        bundle.putString("source","FEED_SCREEN")

        fragment.arguments = bundle
//        fragmentTransaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out, R.anim.fade_in,R.anim.fade_out)
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.addToBackStack(ProfileFragment.TAG)
        fragmentTransaction.commit()
    }

    fun onSearchPressed() {
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_SEARCH"))
        val fragment=SearchFragment()
        fragment?.apply {
            exitTransition = MaterialSharedAxis(
                MaterialSharedAxis.Z,
                /* forward= */ false
            ).apply {
                duration = 500
            }
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()

    }

    private fun initView() {
//        if(User.getInstance().profilePicUrl!=null) {
//            User.getInstance().apply {
//                profilePicUrl?.let { binding.profileIv.setImage(it, radius = 16) }
//            }
//        }

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setBadgeDrawable(callRequestCount: Int) {
        Timber.tag("profilebadge").d(
            "setBadgeDrawable() called with: raisedHandAudienceSize = $callRequestCount"
        )

        if(callRequestCount<100)
        binding.requestCountNumber.text= callRequestCount.toString()
        else
            binding.requestCountNumber.text="9+"
        if(callRequestCount>0 && User.getInstance().isSpeaker && viewModel.isSpeaker.value==true)
            binding.requestCountView.visibility=View.VISIBLE
        else
            binding.requestCountView.visibility=View.INVISIBLE

    }

    private fun addObserver() {

        viewModel.isSpeaker.observe(this){
            if(it)
            {
                viewModel.readRequestCount()
                setBadgeDrawable(viewModel.roomRequestCount.value!!)
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
            else {
                viewModel.requestChannelEnd()
                viewModel.roomRequestCount.value=0
            }
        }

        profileViewModel.openProfile.observe(this){
            val fragment = ProfileFragment() // replace your custom fragment class
            val bundle = Bundle()
            val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            bundle.putString("user", it) // use as per your need
            fragment?.apply {
                exitTransition = MaterialSharedAxis(
                    MaterialSharedAxis.Z,
                    /* forward= */ false
                ).apply {
                    duration = 500
                }
            }
                bundle.putString("source","FANS_LIST")
            fragment.arguments = bundle
            fragmentTransaction.add(R.id.room_frame, fragment)
            fragmentTransaction.commit()
        }

        liveRoomViewModel.pubNubState.observe(this,androidx.lifecycle.Observer{
            if(it==PubNubState.ENDED)
                viewModel.pubNubState=PubNubState.ENDED
            else
                viewModel.pubNubState=PubNubState.STARTED
        })

        viewModel.roomRequestCount.observe(this){
            setBadgeDrawable(it)
        }

        viewModel.previousRoomData.observe(this){
            PreviousRoomDialog(
                this,
                roomName = it.roomName ?: "",
                onNegativeButtonClick = {
                    viewModel.endPreviousRoom(it.roomId, this)
                },
                onPositiveButtonClick = {
                    viewModel.joinRoom(it.roomId.toString(), it.roomName!!, "FEED_SCREEN", isRejoin = true)
                }
            ).apply {
                show()
            }
        }

        viewModel.previousRoomDataForSchedule.observe(this){
            PreviousRoomDialog(
                this,
                roomName = it.previousRoomTopic,
                onPositiveButtonClick = {
                    viewModel.joinRoom(it.previousRoomId.toString(), it.previousRoomTopic, "FEED_SCREEN", isRejoin = true)
                },
                onNegativeButtonClick = {
                    viewModel.endPreviousRoomAndSchedule(it.previousRoomId, this)
                }
            ).apply {
                show()
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

                OPEN_WAIT_ROOM->{
                    it.data?.let{

                        viewModel.pubChannelName?.let { it1 -> PubNubManager.warmUpChannel(it1) }
                        //viewModel.pubChannelName?.let { it1 -> PubNubManager.warmUpChannel(channelName = it1) }
                        viewModel.reader()
                        WaitingFragment.open(this)
                    }
                }
                ROOM_EXPAND->{
                    liveRoomViewModel.liveRoomState.value = LiveRoomState.EXPANDED
                    var msg=Message()
                    msg.what=ROOM_COLLAPSE
                    viewModel.singleLiveEvent.value= msg
                }

                SCROLL_TO_TOP -> {
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
        viewModel.roomData=room
        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_JOIN"))
        viewModel.source="Feed"
        var moderatorId=room.speakersData?.userId
        room.speakersData?.fullName?.let {
            viewModel.speakerName = it
        }
        takePermissions(room.roomId.toString(), room.topic,moderatorId)
    }

    override fun playRoom(room: RoomListResponseItem, view: View) {
        viewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_REPLAY"))
        viewModel.userRoomRecord(room.recordings?.get(0)?.id!!,User.getInstance().userId)
//        RecordedRoomFragment.open(this,"Feed", room)
        RecordedRoomFragment.open(this,"Feed", room, viewModel)
    }

    private fun takePermissions(
        roomId: String? = null,
        roomTopic: String? = null,
        moderatorId: String?
    ) {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            if (roomId == null) {
                openCreateRoomDialog()
            } else viewModel.joinRoom(roomId, roomTopic!!,"FEED_SCREEN",)
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
                            } else viewModel.joinRoom(
                                roomId,
                                roomTopic!!,
                                "FEED_SCREEN",
                            )
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

    override fun onStart() {
        super.onStart()
        viewModel.getRoomRequestCount()
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
//        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_SET_REMINDER"))
//        showPopup(room.roomId,User.getInstance().userId)
        Timber.d("ROOM KA STARTING TIME => ${room.currentTime}")

        notificationScheduler.scheduleNotificationAsListener(this, room)
        viewModel.setReminder(
            ReminderRequest(
                roomId = room.roomId.toString(),
                userId = User.getInstance().userId,
                reminderTime = room.startTimeDate,
                false,
                "FEED_SCREEN"
            )
        )
    }

    fun showPopup(roomId: Int, userId: String) {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(binding.feedRoot.context)
        val dialogBinding = WhyRoomBinding.inflate(layoutInflater)
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
                }
            }
        }
        dialogBinding.Skip.setOnClickListener {
            alertDialog.dismiss()
        }

    }

    override fun viewProfile(profile: String?, deeplink: Boolean, requestDialog: Boolean) {
        val fragment = ProfileFragment() // replace your custom fragment class
        val bundle = Bundle()
        fragment?.apply {
            exitTransition = MaterialSharedAxis(
                MaterialSharedAxis.Z,
                /* forward= */ false
            ).apply {
                duration = 500
            }
        }
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
//        fragmentTransaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out, R.anim.fade_in,R.anim.fade_out)
        bundle.putString("user", profile) // use as per your need
        if(deeplink)
        bundle.putString("source", "deeplink")
        else
            bundle.putString("source","feed")
        bundle.putBoolean("request_dialog",requestDialog)

        fragment.arguments = bundle
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }

    override fun viewRoom(room: RoomListResponseItem, view: View,deeplink: Boolean) {

        profileViewModel.sendEvent(Impression("FEED_SCREEN","CLICKED_CARD"))
        val fragment = ProfileFragment() // replace your custom fragment class

        val bundle = Bundle()
        fragment?.apply {
            exitTransition = MaterialSharedAxis(
                MaterialSharedAxis.Z,
                /* forward= */ false
            ).apply {
                duration = 500
            }
        }
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
//        fragmentTransaction.setCustomAnimations(R.anim.fade_in,R.anim.fade_out, R.anim.fade_in,R.anim.fade_out)
        bundle.putString("user", room.speakersData?.userId) // use as per your need
        if(deeplink)
            bundle.putString("source", "DEEPLINK")
        else
            bundle.putString("source", "FEED_SCREEN")
        fragment.arguments = bundle
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()

    }

    override fun onRoomCreated(conversationRoomResponse: ConversationRoomResponse, topic: String) {
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
    }

    override fun onRoomSchedule(room: RoomListResponseItem) {
        notificationScheduler.scheduleNotificationsForSpeaker(this@FeedActivity, room)
    }

    override fun onError(error: String) {
        showToast(error)
    }

}
