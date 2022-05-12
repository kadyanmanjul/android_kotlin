package com.joshtalks.badebhaiya.liveroom

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.collection.arraySetOf
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.base.BaseFragment
import com.joshtalks.badebhaiya.databinding.FragmentLiveRoomBinding
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.NotificationView
import com.joshtalks.badebhaiya.feed.ROOM_DETAILS
import com.joshtalks.badebhaiya.feed.TOPIC
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.adapter.AudienceAdapter
import com.joshtalks.badebhaiya.liveroom.adapter.SpeakerAdapter
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheet
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheetAction
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheetInfo
import com.joshtalks.badebhaiya.liveroom.bottomsheet.RaisedHandsBottomSheet
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomListingNavigation
import com.joshtalks.badebhaiya.liveroom.model.StartingLiveRoomProperties
import com.joshtalks.badebhaiya.liveroom.service.ConversationRoomCallback
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService
import com.joshtalks.badebhaiya.liveroom.viewmodel.LiveRoomViewModel
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_BOOLEAN
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_ID
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_NAME
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_TYPE
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_USER
import com.joshtalks.badebhaiya.notifications.HeadsUpNotificationService
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.pubnub.PubNubEventsManager
import com.joshtalks.badebhaiya.pubnub.PubNubManager
import com.joshtalks.badebhaiya.pubnub.PubNubState
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setImage
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.IRtcEngineEventHandler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_live_room.*
import kotlinx.android.synthetic.main.li_audience_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class LiveRoomFragment : BaseFragment<FragmentLiveRoomBinding, LiveRoomViewModel>(
    R.layout.fragment_live_room
),
    NotificationView.NotificationViewAction,
    RaisedHandsBottomSheet.HandRaiseSheetListener {

    private val FeedViewModel by lazy {
        ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
    }

    private var mServiceBound: Boolean = false
    private lateinit var binding: FragmentLiveRoomBinding
    private var mBoundService: ConvoWebRtcService? = null
    private var isActivityOpenFromNotification: Boolean = false
    private var roomId: Int? = null
    private var roomQuestionId: Int? = null
    private var isRoomUserSpeaker: Boolean = false
    private var speakerAdapter: SpeakerAdapter? = null
    private var audienceAdapter: AudienceAdapter? = null
    private var channelName: String? = null
    private var channelTopic: String? = null
    private var token: String? = null
    private var iSSoundOn = true
    private var isBottomSheetVisible = false
    private var isHandRaised = true
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isInviteRequestComeFromModerator: Boolean = false
    private var isBackPressed: Boolean = false
    private var isExitApiFired: Boolean = false
    private var backPressCallback: OnBackPressedCallback? = null
    private val vm by lazy { ViewModelProvider(requireActivity()).get(LiveRoomViewModel::class.java) }
    val speakingListForGoldenRing: androidx.collection.ArraySet<Int?> = arraySetOf()

    private val badgeDrawable: BadgeDrawable by lazy { BadgeDrawable.create(requireActivity()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachBackPressedDispatcher()
        ConvoWebRtcService.initLibrary()
        removeIncomingNotification()
        isBackPressed = false

    }

    override fun onInitDataBinding(viewBinding: FragmentLiveRoomBinding) {
        // View is initialized
        viewBinding.handler = this
        binding = viewBinding
        vm.lvRoomState = LiveRoomState.EXPANDED
        channelName = PubNubManager.getLiveRoomProperties()?.channelName
        trackLiveRoomState()
        isActivityOpenFromNotification =
            PubNubManager.getLiveRoomProperties()?.isActivityOpenFromNotification!!
        addViewModelObserver()
        addObserver()
        if (isActivityOpenFromNotification) {
            addJoinAPIObservers()
            getIntentExtrasFromNotification()
        }
        else {
            initData()
            vm.startRoom()
        }
    }

    private fun trackLiveRoomState(){
        binding.liveRoomRootView.addTransitionListener(object : MotionLayout.TransitionListener{
            override fun onTransitionStarted(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int
            ) {
            }

            override fun onTransitionChange(
                motionLayout: MotionLayout?,
                startId: Int,
                endId: Int,
                progress: Float
            ) {
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                if (currentId == R.id.collapsed){
                    vm.lvRoomState = LiveRoomState.COLLAPSED
                } else {
                    vm.lvRoomState = LiveRoomState.EXPANDED
                }
            }

            override fun onTransitionTrigger(
                motionLayout: MotionLayout?,
                triggerId: Int,
                positive: Boolean,
                progress: Float
            ) {
            }

        })
    }

    private fun addViewModelObserver() {
        vm.audienceList.observe(this, androidx.lifecycle.Observer {
            val list = it.sortedBy { it.sortOrder }
            audienceAdapter?.updateFullList(list)
            PubNubManager.getLiveRoomProperties().let {
                if (it.isModerator){
                    val int = vm.getRaisedHandAudienceSize()
                    setBadgeDrawable(int)
                }
            }

        })

        vm.speakersList.observe(this, androidx.lifecycle.Observer {
            val list = it.sortedBy { it.sortOrder }
            speakerAdapter?.updateFullList(list)
        })

        vm.liveRoomState.observe(this){
            when(it){
                LiveRoomState.EXPANDED -> expandLiveRoom()
                LiveRoomState.COLLAPSED -> {}
            }
        }


        vm.singleLiveEvent.observe(this, androidx.lifecycle.Observer {
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                HIDE_PROGRESSBAR -> {
                    hideProgressBar()
                }
                HIDE_SEARCHING_STATE -> {
                }
                LEAVE_ROOM -> {
                    mBoundService?.leaveRoom(roomId, roomQuestionId)
                    isExitApiFired = true
                    vm.unSubscribePubNub()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                SHOW_NOTIFICATION_FOR_INVITE_SPEAKER -> {
                    it.data?.let {
                        val name = it.getString(NOTIFICATION_NAME)
                        val id = it.getInt(NOTIFICATION_ID)
                        val type =
                            it.getParcelable<NotificationView.ConversationRoomNotificationState>(
                                NOTIFICATION_TYPE
                            )
                        setNotificationBarFieldsWithActions(
                            "Dismiss", "Invite to speak", String.format(
                                "\uD83D\uDC4B %s has something to say. Invite " +
                                        "them as speakers?",
                                name
                            ), id,
                            type
                        )
                    }

                }
                SHOW_NOTIFICATION_FOR_USER_TO_JOIN -> {
                    setNotificationBarFieldsWithActions(
                        "Maybe later?", "Join as speaker", String.format(
                            "\uD83D\uDC4B %s invited you to join as a speaker",
                            PubNubManager.moderatorName
                        ), PubNubManager.moderatorUid,
                        NotificationView.ConversationRoomNotificationState.JOIN_AS_SPEAKER
                    )
                }
                CHANGE_MIC_STATUS -> {
                    it.data?.let {
                        val id = it.getInt(NOTIFICATION_ID)
                        val boolean = it.getBoolean(NOTIFICATION_BOOLEAN, true)

                        if (PubNubManager.getLiveRoomProperties()?.agoraUid == id) {
                            iSSoundOn = boolean
                            vm.setChannelMemberStateForUuid(
                                PubNubManager.currentUser,
                                iSSoundOn,
                                channelName
                            )
                            CoroutineScope(Dispatchers.Main).launch {
                                updateMuteButtonState()
                            }
                        }
                    }

                }
                MOVE_TO_SPEAKER -> {
                    it.data?.let {
                        val user = it.getParcelable<LiveRoomUser>(NOTIFICATION_USER)
                        if (PubNubManager.getLiveRoomProperties()?.agoraUid == user?.id) {
                            updateUiWhenSwitchToSpeaker(user?.isMicOn ?: false)
                        }
                        PubNubManager.getLiveRoomProperties()?.isModerator?.let { isModerator ->
                            if (isModerator) {
                                val name = it.getString(NOTIFICATION_NAME)
                                setNotificationWithoutAction(
                                    String.format(
                                        "%s is now a speaker!",
                                        name
                                    ), true,
                                    NotificationView.ConversationRoomNotificationState.HAND_RAISED
                                )
                            }
                        }

                        vm.setChannelMemberStateForUuid(user, channelName = channelName)
                    }
                }
                MOVE_TO_AUDIENCE -> {
                    it.data?.let {
                        val user = it.getParcelable<LiveRoomUser>(NOTIFICATION_USER)
                        if (PubNubManager.getLiveRoomProperties()?.agoraUid == user?.id) {
                            updateUiWhenSwitchToListener()
                        }
                        vm.setChannelMemberStateForUuid(user, channelName = channelName)
                    }
                }
            }
        })
    }

    fun collapseLiveRoom(){
        binding.liveRoomRootView.transitionToEnd()
//        vm.lvRoomState = LiveRoomState.COLLAPSED
    }

     fun expandLiveRoom() {
         Log.i("YASHEN", "expandLiveRoom: ")
        binding.liveRoomRootView.transitionToStart()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setBadgeDrawable(raisedHandAudienceSize: Int) {
        Log.d(
            "Manjul",
            "setBadgeDrawable() called with: raisedHandAudienceSize = $raisedHandAudienceSize"
        )
        badgeDrawable.setNumber(raisedHandAudienceSize)
        badgeDrawable.horizontalOffset = 20
        badgeDrawable.verticalOffset = 20
        BadgeUtils.attachBadgeDrawable(badgeDrawable, binding.raisedHands)
        badgeDrawable.setVisible(raisedHandAudienceSize>0)

    }


    private fun endRoom() {
        Timber.tag("ABC2").e("endRoom() called")
        mBoundService?.endRoom(roomId, roomQuestionId)
        isExitApiFired = true
        vm.unSubscribePubNub()
        requireActivity().supportFragmentManager.popBackStack()
    }


    private fun initData() {
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        //TODO init data to adapters

        if (PubNubManager.getLiveRoomProperties().isRoomCreatedByUser) {
            Log.d("lvroom", "initData: is Created by user")
            updateMuteButtonState()
        }
        else {
            Log.d("lvroom", "not created by user")
            binding.apply {
                muteBtn.visibility = View.VISIBLE
                muteBtn.isEnabled = false
                unmuteBtn.visibility = View.GONE
                handUnraiseBtn.visibility = View.VISIBLE
                handRaiseBtn.visibility = View.GONE
            }
        }
        handler = Handler(Looper.getMainLooper())
        updateUI()
        leaveRoomIfModeratorEndRoom()
        clickListener()
        takePermissions()
    }

    private fun addJoinAPIObservers() {
        showProgressBar()
        vm.navigation.observe(this) {
            try {
                when (it) {
                    is ConversationRoomListingNavigation.ApiCallError -> showApiCallErrorToast(it.error)
                    is ConversationRoomListingNavigation.OpenConversationLiveRoom -> setValues(
                        it.channelName,
                        it.uid,
                        it.token,
                        it.isRoomCreatedByUser,
                        it.roomId
                    )
                    else -> {
                        hideProgressBar()
                    }
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    private fun setValues(
        channelName: String?,
        uid: Int?,
        token: String?,
        roomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        Log.d(
            "ABC2",
            "setValues() called with: channelName = $channelName, uid = $uid, token = $token, roomCreatedByUser = $roomCreatedByUser, roomId = $roomId"
        )
        this.channelName = channelName
        uid?.let {
            PubNubManager.getLiveRoomProperties().agoraUid = it
        }
        this.token = token
        this.roomId = roomId
        this.roomQuestionId = null
        initData()
        vm.startRoom()
        hideProgressBar()
    }

    private fun showApiCallErrorToast(error: String) {
        hideProgressBar()
        if (error.isNotEmpty()) {
            binding.notificationBar.apply {
                visibility = View.VISIBLE
                setNotificationState(NotificationView.ConversationRoomNotificationState.API_ERROR)
                hideActionLayout()
                setHeading(error)
                setBackgroundColor(false)
                loadAnimationSlideDown()
                startSound()
                hideNotificationAfter4seconds()
            }
        }
        else {
            showToast("Something Went Wrong !!!")
        }
    }

    private fun getIntentExtrasFromNotification() {
        roomId = PubNubManager.getLiveRoomProperties().roomId
        channelTopic = PubNubManager.getLiveRoomProperties().channelTopic
        if (isActivityOpenFromNotification && roomId != null) {
            vm.joinRoom(
                RoomListResponseItem(
                    roomId!!,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )

            )
        }
    }


    private fun callWebRtcService() {
        Log.d(
            "ABC2",
            "conversationRoomJoin() called with: token = $token, channelName = $channelName, uid = ${PubNubManager.getLiveRoomProperties().agoraUid}, moderatorId = ${PubNubManager.getLiveRoomProperties().moderatorId}, channelTopic = $channelTopic, roomId = $roomId, roomQuestionId = $roomQuestionId"
        )
        PubNubManager.callWebRtcService()
    }

    private fun removeIncomingNotification() {
        val notificationManager =
            AppObjectController.joshApplication.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(9999)
        try {
            requireActivity().stopService(Intent(requireActivity(), HeadsUpNotificationService::class.java))
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private var callbackOld: ConversationRoomCallback = object : ConversationRoomCallback {
        override fun onUserOffline(uid: Int) {
            removeUserWhenLeft(uid)
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {

            val uids = ArrayList<Int>()
            speakers?.forEach { user ->
                if (user.volume <= 2) {
                    when (user.uid) {
                        0 -> speakingListForGoldenRing.remove(PubNubManager.getLiveRoomProperties().agoraUid)
                        else -> speakingListForGoldenRing.remove(user.uid)
                    }
                }
                else if (user.volume > 2) {
                    when (user.uid) {
                        0 -> uids.add(PubNubManager.getLiveRoomProperties().agoraUid)
                        else -> uids.add(user.uid)
                    }
                }
            }
            refreshSpeakingUsers(uids)
        }

        override fun onSwitchToSpeaker() {
            //TODO("Not yet implemented")
        }

        override fun onSwitchToAudience() {
            //TODO("Not yet implemented")
        }

    }
    fun addObserver(){
        FeedViewModel.singleLiveEvent.observe(viewLifecycleOwner) {
            Log.i("ABC2", "addObserver: ${it.what}")
            Log.d("ABC2", "Data class called with data message: ${it.what} bundle : ${it.data}")
            when (it.what) {
                OPEN_ROOM ->{
                    Log.i("YASHENDRA", "addObserver: ")
                    it.data?.let {
                        it.getParcelable<ConversationRoomResponse>(ROOM_DETAILS)?.let { room ->
                            val liveRoomProperties = StartingLiveRoomProperties.createFromRoom(
                                room,
                                it.getString(TOPIC)!!
                            )
                            launch((requireActivity() as AppCompatActivity), liveRoomProperties, vm)
                        }
                    }
                }
            }
        }
    }



    private fun removeUserWhenLeft(uid: Int) {
        PubNubManager.removeUserWhenLeft(uid, speakerAdapter, audienceAdapter)
    }

    private fun refreshSpeakingUsers(uids: List<Int?>) {
        speakingListForGoldenRing.addAll(uids)
        var i = 0
        for (speaker in vm.getSpeakerList()) {
            val viewHolder = binding.speakersRecyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is SpeakerAdapter.SpeakerViewHolder) {
                viewHolder.setGoldenRingVisibility(speakingListForGoldenRing.contains(speaker.id))
            }
            i++
        }
    }

    private var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("ABC", "onServiceConnected() called with: name = $name, service = $service")
            val myBinder = service as ConvoWebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callbackOld)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("ABC", "onServiceDisconnected() ")
            mServiceBound = false
        }

    }

    private fun updateMuteButtonState() {
        when (iSSoundOn) {
            true -> {
                binding.unmuteBtn.visibility = View.VISIBLE
                binding.muteBtn.visibility = View.VISIBLE
                mBoundService?.unMuteCall()
            }
            false -> {
                binding.unmuteBtn.visibility = View.GONE
                binding.muteBtn.visibility = View.VISIBLE
                binding.muteBtn.isEnabled = true
                mBoundService?.muteCall()
            }
        }
    }


    private fun updateUI() {
        setUpRecyclerView()
        setLeaveEndButton(PubNubManager.getLiveRoomProperties().isRoomCreatedByUser)
        if(User.getInstance().profilePicUrl!=null) {
            User.getInstance().apply {
                profilePicUrl?.let { binding.userPhoto.setImage(it, radius = 16) }
                //binding.profileIv.setUserImageOrInitials(profilePicUrl, firstName.toString())
            }
        }
        binding.userPhoto.clipToOutline = true
//        binding.userPhoto.setUserImageRectOrInitials(
//            User.getInstance().profilePicUrl,
//            User.getInstance().firstName ?: DEFAULT_NAME,
//            textColor = R.color.black,
//            bgColor = R.color.conversation_room_gray
//        )


        binding.topic.text = PubNubManager.getLiveRoomProperties().channelTopic


        if (PubNubManager.getLiveRoomProperties().isRoomCreatedByUser) {
            binding.handRaiseBtn.visibility = View.GONE
            binding.raisedHands.visibility = View.VISIBLE
        }
        else {
            binding.handRaiseBtn.visibility = View.GONE
            binding.handUnraiseBtn.visibility = View.VISIBLE
            binding.handUnraiseBtn.isEnabled = true
            binding.raisedHands.visibility = View.GONE
        }
    }

    private fun clickListener() {

        binding.leaveEndRoomBtn.setOnSingleClickListener {
            if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
                showEndRoomPopup()
            }
            else {
                showLeaveRoomPopup()
            }
        }

        binding.userPhoto.setOnClickListener {
            collapseLiveRoom()
            itemClick(User.getInstance().userId)
        }
        binding.muteBtn.setOnClickListener {
            if (internetAvailableFlag) {
                when (iSSoundOn) {
                    true -> changeMuteButtonState(false)
                    false -> changeMuteButtonState(true)
                }
            }
            else {
                internetNotAvailable()
            }
        }
        binding.unmuteBtn.setOnClickListener {
            if (internetAvailableFlag) {
                when (iSSoundOn) {
                    true -> changeMuteButtonState(false)
                    false -> changeMuteButtonState(true)
                }
            }
            else {
                internetNotAvailable()
            }
        }

        binding.handRaiseBtn.setOnClickListener {
            if (internetAvailableFlag) {
                when (isHandRaised) {
                    true -> clickHandRaisedButton(true, "HAND_RAISED")
                    false -> clickHandRaisedButton(false, "HAND_UNRAISED")
                }
            }
            else {
                internetNotAvailable()
            }
        }
        binding.handUnraiseBtn.setOnClickListener {
            if (internetAvailableFlag) {
                when (isHandRaised) {
                    true -> clickHandRaisedButton(true, "HAND_RAISED")
                    false -> clickHandRaisedButton(false, "HAND_UNRAISED")
                }
            }
            else {
                internetNotAvailable()
            }
        }

        binding.raisedHands.setOnSingleClickListener {
            if (internetAvailableFlag) {
                openRaisedHandsBottomSheet()
            }
            else {
                internetNotAvailable()
            }
        }
    }

    private fun clickHandRaisedButton(isRaised: Boolean, type: String) {
        try {
            isHandRaised = !isRaised
            when (isRaised) {
                true -> {
                    binding.apply {
                        handRaiseBtn.visibility = View.VISIBLE
                        handUnraiseBtn.visibility = View.GONE
                        listener__recycler_view.raised_hands.visibility=View.VISIBLE
                    }
                    setNotificationWithoutAction(
                        String.format(
                            "\uD83D\uDC4B You raised your hand! We’ll let the speakers\n" +
                                    "know you want to talk..."
                        ), true,
                        NotificationView.ConversationRoomNotificationState.YOUR_HAND_RAISED
                    )
                }
                false -> {
                    binding.apply {
                        handRaiseBtn.visibility = View.GONE
                        handUnraiseBtn.visibility = View.VISIBLE
                        listener__recycler_view.raised_hands.visibility=View.GONE
                    }
                }
            }

            PubNubEventsManager.sendHandRaisedEvent(isRaised)

        }
        catch (ex: Exception) {
            showToast(ex.toString())
        }
    }

    private fun changeMuteButtonState(isMicOn: Boolean) {
        PubNubEventsManager.sendMuteEvent(isMicOn)
    }


    private fun setNotificationBarFieldsWithActions(
        rejectedText: String,
        acceptedText: String,
        heading: String,
        userUid: Int?,
        state: NotificationView.ConversationRoomNotificationState? = NotificationView.ConversationRoomNotificationState.DEFAULT
    ) {
        CoroutineScope(Dispatchers.Main).launch {

            binding.notificationBar.apply {
                if (getNotificationState() != NotificationView.ConversationRoomNotificationState.HAND_RAISED) {
                    setUserUuid(userUid)
                    setNotificationState(state!!)
                    visibility = View.VISIBLE
                    showActionLayout()
                    setRejectButtonText(rejectedText)
                    setAcceptButtonText(acceptedText)
                    setHeading(heading)
                    startSound()
                    setBackgroundColor(true)
                    loadAnimationSlideDown()
                }
            }
            if (runnable != null) {
                handler?.removeCallbacks(runnable!!)
            }
        }
    }

    private fun hideNotificationAfter4seconds() {
        if (runnable == null) {
            setRunnable()
            handler?.postDelayed(runnable!!, 4000)
        }
        else {
            handler?.removeCallbacks(runnable!!)
            setRunnable()
            handler?.postDelayed(runnable!!, 4000)
        }
    }

    private fun setRunnable() {
        runnable = Runnable {
            binding.notificationBar.apply {
                loadAnimationSlideUp()
                endSound()
            }
        }
    }


    private fun setNotificationWithoutAction(
        heading: String,
        isGreenColorNotification: Boolean,
        state: NotificationView.ConversationRoomNotificationState
    ) {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            setNotificationState(state)
            hideActionLayout()
            setHeading(heading)
            setBackgroundColor(isGreenColorNotification)
            startSound()
            loadAnimationSlideDown()
        }
        hideNotificationAfter4seconds()
    }

    override fun onAcceptNotification() {
        if (PubNubManager.getLiveRoomProperties().isRoomCreatedByUser) {
            binding.notificationBar.getUserUuid()?.let {
                PubNubEventsManager.sendInviteUserEvent(it)
            }
            binding.notificationBar.loadAnimationSlideUp()
        }
        else {
            PubNubEventsManager.moveToSpeakerEvent()
            isInviteRequestComeFromModerator = true
            binding.notificationBar.loadAnimationSlideUp()
        }

    }

    override fun onRejectNotification() {
        if (PubNubManager.getLiveRoomProperties().isRoomCreatedByUser.not()) {
            PubNubEventsManager.removeHandRaise()
            isHandRaised = !isHandRaised
            binding.apply {
                handRaiseBtn.visibility = View.GONE
                handUnraiseBtn.visibility = View.VISIBLE
            }
        }
        binding.notificationBar.loadAnimationSlideUp()
    }

    private fun showRoomEndNotification(roomId: Int?) {
        Log.d("ABC2", "showRoomEndNotification() called with: roomId = $roomId")
        if (this.roomId == roomId) {
            binding.notificationBar.apply {
                visibility = View.VISIBLE
                hideActionLayout()
                setBackgroundColor(false)
                setHeading("This room has ended")
                //startSound()
                loadAnimationSlideDown()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                vm.unSubscribePubNub()
                finishFragment()
            }, 4000)
        }
    }

    private fun updateUiWhenSwitchToListener() {
        isRoomUserSpeaker = false
        mBoundService?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
        //mBoundService?.muteCall()
        binding.apply {
            muteBtn.visibility = View.VISIBLE
            muteBtn.isEnabled = false
            unmuteBtn.visibility = View.GONE
            handRaiseBtn.visibility = View.GONE
            handUnraiseBtn.visibility = View.VISIBLE
            handUnraiseBtn.isEnabled = true
        }
        isInviteRequestComeFromModerator = false
    }

    private fun updateUiWhenSwitchToSpeaker(isMicOn: Any?) {
        isRoomUserSpeaker = true
        isInviteRequestComeFromModerator = true
        mBoundService?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        binding.handRaiseBtn.visibility = View.GONE
        binding.handUnraiseBtn.visibility = View.VISIBLE
        binding.handUnraiseBtn.isEnabled = false
        isHandRaised = true
        iSSoundOn = isMicOn == true
        updateMuteButtonState()
        when (iSSoundOn) {
            true -> mBoundService?.unMuteCall()
            false -> mBoundService?.muteCall()
        }
    }

    private fun leaveRoomIfModeratorEndRoom() {


    }

    private fun takePermissions() {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(requireActivity())) {
            callWebRtcService()
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            callWebRtcService()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            showToast("Permission Denied ")
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(requireActivity())
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

    private fun observeNetwork() {
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(requireActivity().applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    Log.d("ABC2", "observeNetwork() called with: connectivity = $connectivity")
                    internetAvailableFlag =
                        connectivity.state() == NetworkInfo.State.CONNECTED && connectivity.available()
                    if (internetAvailableFlag) {
                        internetAvailable()
                        vm.reconnectPubNub()
                    }
                    else {
                        internetNotAvailable()
                    }
                }
        )
    }

    private fun internetNotAvailable() {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            setHeading("The Internet connection appears to be offline")
           var int= Intent(requireContext(), FeedActivity::class.java)
            startActivity(int)
            finishFragment()
            setNotificationState(NotificationView.ConversationRoomNotificationState.NO_INTERNET_AVAILABLE)
            loadAnimationSlideDown()
            startSound()
            hideActionLayout()
            setBackgroundColor(false)
        }
    }

    private fun internetAvailable() {
        binding.notificationBar.apply {
            visibility = View.GONE
            setNotificationState(NotificationView.ConversationRoomNotificationState.DEFAULT)
            endSound()
        }
    }

    private fun setLeaveEndButton(isRoomCreatedByUser: Boolean) {
        when (isRoomCreatedByUser) {
            true -> {
                binding.leaveEndRoomBtn.text = getString(R.string.end_room)
            }
            false -> {
                binding.leaveEndRoomBtn.text = getString(R.string.leave_room)
            }
        }
        binding.leaveEndRoomBtn.visibility = View.VISIBLE
    }

    private fun setUpRecyclerView() {
        speakerAdapter =
            SpeakerAdapter()
        audienceAdapter =
            AudienceAdapter(PubNubManager.getLiveRoomProperties().isRoomCreatedByUser)

        binding.speakersRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = speakerAdapter
        }

        binding.listenerRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = audienceAdapter
        }

        audienceAdapter?.setOnItemClickListener(object : AudienceAdapter.OnUserItemClickListener {

            override fun onItemClick(user: LiveRoomUser) {
                if (activity?.supportFragmentManager?.backStackEntryCount!! > 0 && isBottomSheetVisible.not()) {
                    getDataOnSpeakerAdapterItemClick(user, user.id, false)
                    isBottomSheetVisible = true
                }
            }
        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(user: LiveRoomUser) {
                if (activity?.supportFragmentManager?.backStackEntryCount!! > 0 && isBottomSheetVisible.not()) {
                    getDataOnSpeakerAdapterItemClick(user, user.id, true)
                    isBottomSheetVisible = true
                }
            }
        })

    }

    private fun getDataOnSpeakerAdapterItemClick(
        user: LiveRoomUser?, userUid: Int?,
        toSpeaker: Boolean
    ) {
        val roomInfo = ConversationRoomBottomSheetInfo(
            PubNubManager.getLiveRoomProperties().isRoomCreatedByUser,
            isRoomUserSpeaker,
            toSpeaker,
            user?.name ?: "",
            user?.photoUrl ?: "",
            userUid == PubNubManager.getLiveRoomProperties().agoraUid
        )
        showBottomSheet(
            roomInfo,
            user?.userId!!,
            userUid,
            user?.name.toString()
        )
    }

    private fun showBottomSheet(
        roomInfo: ConversationRoomBottomSheetInfo,
        mentorId: String,
        userUid: Int?,
        userName: String
    ) {
        val bottomSheet =
            ConversationRoomBottomSheet.newInstance(roomInfo,
                object : ConversationRoomBottomSheetAction {
                    override fun openUserProfile() {
                        if (mentorId.isBlank().not()) {
                            collapseLiveRoom()
                            itemClick(mentorId)
                        }
                        else {
                            showToast(getString(R.string.generic_message_for_error))
                        }
                    }

                    override fun moveToAudience() {

                        PubNubEventsManager.moveToAudience(userUid.toString(), userName)

                    }

                    override fun moveToSpeaker() {
                        if (vm.getSpeakerList().size < 16) {
                            PubNubEventsManager.inviteToSpeakerWithMicOff(userUid.toString())
                        }
                        else {
                            setNotificationWithoutAction(
                                "Room has reached maximum allowed number of speakers." +
                                        " Please try again after sometime.", false,
                                NotificationView.ConversationRoomNotificationState.MAX_LIMIT_REACHED
                            )
                        }
                    }

                    override fun onDismiss() {
                        isBottomSheetVisible = false
                    }
                })
        bottomSheet.show(requireActivity().supportFragmentManager, "Bottom sheet")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    private fun openUserProfile(mentorId: String) {
        ProfileActivity.openProfileActivity(requireActivity(), mentorId)
        /* UserProfileActivity.startUserProfileActivity(
             this@ConversationLiveRoomActivity,
             mentorId,
             flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
         )*/
    }

    fun openRaisedHandsBottomSheet() {
        val raisedHandList =
            vm.getAudienceList().filter { it.isSpeaker == false && it.isHandRaised }
        val bottomSheet =
            RaisedHandsBottomSheet.newInstance(
                roomId ?: 0, PubNubManager.getLiveRoomProperties().agoraUid, PubNubManager.moderatorName, PubNubManager.getLiveRoomProperties().channelName,
                ArrayList(raisedHandList),
                this
            )
        binding.notificationBar.loadAnimationSlideUp()
        bottomSheet.show(requireActivity().supportFragmentManager, "Bottom sheet Hands Raised")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    override fun onUserInvitedToSpeak(user: LiveRoomUser) {
        PubNubEventsManager.userInvitedToSpeak(user.id)
        //TODO("Not yet implemented")
    }

    private fun showEndRoomPopup() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_end_room, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        dialogView.findViewById<AppCompatTextView>(R.id.cancel).setOnClickListener {
            isBackPressed = false
            alertDialog.dismiss()
        }

        dialogView.findViewById<AppCompatTextView>(R.id.end_room).setOnClickListener {
            Log.d("ABC2", "activity showEndRoomPopup() called $mBoundService")
            if (!internetAvailableFlag) {
                //viewModel.unSubscribePubNub()
                finishFragment()
            }
            mBoundService?.endRoom(roomId, roomQuestionId)
            isExitApiFired = true
            alertDialog.dismiss()
            vm.unSubscribePubNub()
            finishFragment()
        }
    }

    private fun finishFragment(){
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun showLeaveRoomPopup() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_leave_room, null)
        dialogBuilder.setView(dialogView)

        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.show()

        dialogView.findViewById<AppCompatTextView>(R.id.cancel_leave).setOnClickListener {
            isBackPressed = false
            alertDialog.dismiss()
        }

        dialogView.findViewById<AppCompatTextView>(R.id.leave_room).setOnClickListener {
            Log.d("ABC2", "activity showLeaveRoomPopup() called $mBoundService")
            if (!internetAvailableFlag) {
                finishFragment()
            }
            mBoundService?.leaveRoom(roomId, roomQuestionId)
            isExitApiFired = true
            alertDialog.dismiss()
            vm.unSubscribePubNub()
            finishFragment()
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().bindService(
            Intent(requireActivity(), ConvoWebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        PubNubManager.pauseRoomDataCollection()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        requireActivity().unbindService(myConnection)
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable.clear()
        observeNetwork()
        PubNubManager.collectPubNubEvents()
    }

    private fun handleBackPress(onBackPressedCallback: OnBackPressedCallback) {
        backPressCallback = onBackPressedCallback
        isBackPressed = true
        if (vm.pubNubState.value != null && vm.pubNubState.value == PubNubState.STARTED){
            /** Live Room is Going on */
            if (vm.lvRoomState == LiveRoomState.EXPANDED){
                // Minimise live room.
                collapseLiveRoom()
            } else {
                // Live is already minimized ask if user wants to quit live room.
                if (!internetAvailableFlag) {
                    mBoundService?.endService()
//                    requireActivity().onBackPressed()
                    finishFragment()
                    return
                }
                if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
                    showEndRoomPopup()
                }
                else {
                    showLeaveRoomPopup()
                }
            }
        }
    }

    private fun attachBackPressedDispatcher(){
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackPress(this)
        }
    }

    override fun onDestroy() {
        backPressCallback?.isEnabled = false
        if ((isBackPressed.or(isExitApiFired)).not()) {
            if (PubNubManager.getLiveRoomProperties().isRoomCreatedByUser) {
                mBoundService?.endRoom(roomId, roomQuestionId)
            }
            else {
                mBoundService?.leaveRoom(roomId, roomQuestionId)
            }
        }
        binding.notificationBar.destroyMediaPlayer()
        requireActivity().stopService(Intent(requireActivity(), ConvoWebRtcService::class.java))
        super.onDestroy()

    }

    companion object {
        const val CHANNEL_NAME = "channel_name"
        const val UID = "uid"
        const val MODERATOR_UID = "moderator_uid"
        const val TOKEN = "TOKEN"
        const val IS_ROOM_CREATED_BY_USER = "is_room_created_by_user"
        const val ROOM_ID = "room_id"
        const val OPEN_FROM_NOTIFICATION = "open_from_notification"
        const val ROOM_QUESTION_ID = "room_question_id"
        const val TOPIC_NAME = "topic_name"
        const val TAG = "live_room"


        fun launch(
            activity: AppCompatActivity,
            liveRoomProperties: StartingLiveRoomProperties,
            liveRoomViewModel: LiveRoomViewModel
        ) {
            if (liveRoomViewModel.pubNubState.value == PubNubState.STARTED){
                showToast("Please Leave Current Room")
                //LiveRoomFragment().expandLiveRoom()
                //LiveRoomFragment().expandLiveRoom()
            } else {

                var frag=activity.supportFragmentManager.findFragmentById(R.id.liveRoomRootView)
                if(frag==null) {
                    PubNubManager.warmUp(liveRoomProperties)
                    activity
                        .supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, LiveRoomFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

    }



    override fun getViewModel(): LiveRoomViewModel = vm
     fun itemClick(userId: String) {
        val nextFrag = ProfileActivity()
        val bundle = Bundle()
        bundle.putString("user", userId) // use as per your need
        nextFrag.arguments = bundle
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.root_view, nextFrag, "findThisFragment")
            //?.addToBackStack(null)
            ?.commit()
    }
}