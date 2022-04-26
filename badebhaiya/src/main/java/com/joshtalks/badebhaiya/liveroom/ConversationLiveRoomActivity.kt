package com.joshtalks.badebhaiya.liveroom

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.BaseActivity
import com.joshtalks.badebhaiya.core.LogException
import com.joshtalks.badebhaiya.core.PermissionUtils
import com.joshtalks.badebhaiya.core.setOnSingleClickListener
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.ActivityConversationLiveRoomBinding
import com.joshtalks.badebhaiya.feed.NotificationView
import com.joshtalks.badebhaiya.feed.model.LiveRoomUser
import com.joshtalks.badebhaiya.feed.model.RoomListResponseItem
import com.joshtalks.badebhaiya.liveroom.adapter.AudienceAdapter
import com.joshtalks.badebhaiya.liveroom.adapter.SpeakerAdapter
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheet
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheetAction
import com.joshtalks.badebhaiya.liveroom.bottomsheet.ConversationRoomBottomSheetInfo
import com.joshtalks.badebhaiya.liveroom.bottomsheet.RaisedHandsBottomSheet
import com.joshtalks.badebhaiya.liveroom.model.ConversationRoomListingNavigation
import com.joshtalks.badebhaiya.liveroom.service.ConversationRoomCallback
import com.joshtalks.badebhaiya.liveroom.service.ConvoWebRtcService
import com.joshtalks.badebhaiya.liveroom.viewmodel.ConversationRoomViewModel
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_BOOLEAN
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_ID
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_NAME
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_TYPE
import com.joshtalks.badebhaiya.liveroom.viewmodel.NOTIFICATION_USER
import com.joshtalks.badebhaiya.notifications.HeadsUpNotificationService
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.DEFAULT_NAME
import com.joshtalks.badebhaiya.utils.setUserImageRectOrInitials
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.IRtcEngineEventHandler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ConversationLiveRoomActivity : BaseActivity(),
                                     NotificationView.NotificationViewAction,
                                     RaisedHandsBottomSheet.HandRaiseSheetListener {

    private var mServiceBound: Boolean = false
    private lateinit var binding: ActivityConversationLiveRoomBinding
    private var mBoundService: ConvoWebRtcService? = null
    private var isActivityOpenFromNotification: Boolean = false
    private var roomId: Int? = null
    private var roomQuestionId: Int? = null
    private var isRoomCreatedByUser: Boolean = false
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
    private var isPubNubUsersFetched: Boolean = false
    private val vm by lazy { ViewModelProvider(this).get(ConversationRoomViewModel::class.java) }
    val speakingListForGoldenRing: androidx.collection.ArraySet<Int?> = arraySetOf()

    private val badgeDrawable: BadgeDrawable by lazy { BadgeDrawable.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConvoWebRtcService.initLibrary()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, theme)
        }
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        removeIncomingNotification()
        isBackPressed = false
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isActivityOpenFromNotification =
            intent?.getBooleanExtra(OPEN_FROM_NOTIFICATION, false) == true
        addViewModelObserver()
        if (isActivityOpenFromNotification) {
            addJoinAPIObservers()
            getIntentExtras(intent)
        }
        else {
            getIntentExtras()
            initData()
            vm.initPubNub(channelName)
        }
    }

    private fun addViewModelObserver() {
        vm.audienceList.observe(this, androidx.lifecycle.Observer {
            val list = it.sortedBy { it.sortOrder }
            audienceAdapter?.updateFullList(list)
            if (vm.isModerator()){
                val int = vm.getRaisedHandAudienceSize()
                setBadgeDrawable(int)
            }
        })

        vm.speakersList.observe(this, androidx.lifecycle.Observer {
            val list = it.sortedBy { it.sortOrder }
            speakerAdapter?.updateFullList(list)
        })

        vm.isPubNubUsersFetched.observe(this, androidx.lifecycle.Observer {
            if (it) {
                addPubNubEventObserver()
            }
        })

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
                    finish()
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
                            vm.getModeratorName()
                        ), vm.getModeratorId(),
                        NotificationView.ConversationRoomNotificationState.JOIN_AS_SPEAKER
                    )
                }
                CHANGE_MIC_STATUS -> {
                    it.data?.let {
                        val id = it.getInt(NOTIFICATION_ID)
                        val boolean = it.getBoolean(NOTIFICATION_BOOLEAN, true)
                        if (vm.getAgoraUid() == id) {
                            iSSoundOn = boolean
                            vm.setChannelMemberStateForUuid(
                                vm.getCurrentUser(),
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
                        if (vm.getAgoraUid() == user?.id) {
                            updateUiWhenSwitchToSpeaker(user?.isMicOn ?: false)
                        }
                        if (vm.isModerator()) {
                            val name = it.getString(NOTIFICATION_NAME)
                            setNotificationWithoutAction(
                                String.format(
                                    "%s is now a speaker!",
                                    name
                                ), true,
                                NotificationView.ConversationRoomNotificationState.HAND_RAISED
                            )
                        }
                        vm.setChannelMemberStateForUuid(user, channelName = channelName)
                    }
                }
                MOVE_TO_AUDIENCE -> {
                    it.data?.let {
                        val user = it.getParcelable<LiveRoomUser>(NOTIFICATION_USER)
                        if (vm.getAgoraUid() == user?.id) {
                            updateUiWhenSwitchToListener()
                        }
                        vm.setChannelMemberStateForUuid(user, channelName = channelName)
                    }
                }
            }
        })
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
        finish()
    }


    private fun addPubNubEventObserver() {
        Log.d(
            "ABC2",
            "addPubNubEventObserver() called  isPubNubObserverAdded: ${isPubNubUsersFetched} "
        )
        compositeDisposable.add(vm.getReplayDisposable())
    }

    private fun initData() {
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        //TODO init data to adapters
        Log.d("Manjul", "initData() called $isRoomCreatedByUser")
        if (isRoomCreatedByUser) {
            updateMuteButtonState()
        }
        else {
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
        setNotificationStates()
        leaveRoomIfModeratorEndRoom()
        clickListener()
        switchRoles()
        takePermissions()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        isActivityOpenFromNotification =
            intent?.getBooleanExtra(OPEN_FROM_NOTIFICATION, false) == true
        if (isActivityOpenFromNotification) {
            getIntentExtras(intent)
        }
        val newChannelName = intent?.getStringExtra(CHANNEL_NAME)
        if (newChannelName != null && newChannelName != channelName) {
            vm.unSubscribePubNub()
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
            return
        }
    }

    private fun addJoinAPIObservers() {
        showProgressBar()
        vm.navigation.observe(this, {
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
            }
            catch (ex: Exception) {
                LogException.catchException(ex)
            }
        })
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
        vm.setAgoraUid(uid)
        this.token = token
        this.roomId = roomId
        this.roomQuestionId = null
        this.isRoomCreatedByUser = roomCreatedByUser
        initData()
        vm.initPubNub(channelName)
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
            Toast.makeText(this, "Something Went Wrong !!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getIntentExtras(intent: Intent?) {
        roomId = intent?.getIntExtra(ROOM_ID, 0)?:0
        channelTopic = intent?.getStringExtra(TOPIC_NAME)
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
            "conversationRoomJoin() called with: token = $token, channelName = $channelName, uid = ${vm.getAgoraUid()}, moderatorId = ${vm.getModeratorId()}, channelTopic = $channelTopic, roomId = $roomId, roomQuestionId = $roomQuestionId"
        )
        ConvoWebRtcService.conversationRoomJoin(
            token,
            channelName,
            vm.getAgoraUid(),
            vm.getModeratorId(),
            channelTopic,
            roomId,
            roomQuestionId,
            isRoomCreatedByUser
        )
    }

    private fun removeIncomingNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(9999)
        try {
            stopService(Intent(this, HeadsUpNotificationService::class.java))
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
                        0 -> speakingListForGoldenRing.remove(vm.getAgoraUid()!!)
                        else -> speakingListForGoldenRing.remove(user.uid)
                    }
                }
                else if (user.volume > 2) {
                    when (user.uid) {
                        0 -> uids.add(vm.getAgoraUid()!!)
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

    private fun removeUserWhenLeft(uid: Int) {
        if (vm.getSpeakerList().any { it.id == uid }) {
            val user = vm.getSpeakerList().filter { it.id == uid }
            vm.getSpeakerList().removeAll(user)
            CoroutineScope(Dispatchers.Main).launch {
                speakerAdapter?.updateFullList(ArrayList(vm.getSpeakerList()))
            }
        }
        else if (vm.getAudienceList().any { it.id == uid }) {
            val user = vm.getAudienceList().filter { it.id == uid }
            vm.getAudienceList().removeAll(user)
            CoroutineScope(Dispatchers.Main).launch {
                audienceAdapter?.updateFullList(ArrayList(vm.getAudienceList()))
            }
            vm.updateAudienceList(ArraySet(vm.getAudienceList()))
        }
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
        Log.d("Manjul", "updateMuteButtonState() called $iSSoundOn")
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

    private fun getIntentExtras() {
        channelName = intent?.getStringExtra(CHANNEL_NAME)
        vm.setAgoraUid(intent?.getIntExtra(UID, 0))
        vm.setModeratorId(intent?.getIntExtra(MODERATOR_UID, 0))
        Log.d(
            "ABC2",
            "getIntentExtras() MODERATOR_UID called ${intent?.getIntExtra(MODERATOR_UID, 0)} isModerator : ${intent.getBooleanExtra(IS_ROOM_CREATED_BY_USER, false)}"
        )
        token = intent?.getStringExtra(TOKEN)
        roomId = intent?.getIntExtra(ROOM_ID, 0)
        channelTopic = intent?.getStringExtra(TOPIC_NAME)
        roomQuestionId = intent?.getIntExtra(ROOM_QUESTION_ID, 0)
        isRoomCreatedByUser = intent.getBooleanExtra(IS_ROOM_CREATED_BY_USER, false)
    }

    private fun updateUI() {
        setUpRecyclerView()
        setLeaveEndButton(isRoomCreatedByUser)
        binding.userPhoto.clipToOutline = true
        binding.userPhoto.setUserImageRectOrInitials(
            User.getInstance().profilePicUrl,
            User.getInstance().firstName ?: DEFAULT_NAME,
            textColor = R.color.black,
            bgColor = R.color.conversation_room_gray
        )
        binding.topic.text = channelTopic


        if (isRoomCreatedByUser) {
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
            openUserProfile(User.getInstance().userId)
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
            isHandRaised = !isHandRaised
            when (isRaised) {
                true -> {
                    binding.apply {
                        handRaiseBtn.visibility = View.VISIBLE
                        handUnraiseBtn.visibility = View.GONE
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
                    }
                }
            }
            val customMessage = JsonObject()
            customMessage.addProperty("id", vm.getAgoraUid())
            customMessage.addProperty("is_hand_raised", isRaised)
            customMessage.addProperty("short_name", vm.getCurrentUser()?.name ?: DEFAULT_NAME)
            customMessage.addProperty("action", "IS_HAND_RAISED")
            vm.sendCustomMessage(customMessage, vm.getModeratorId().toString())

        }
        catch (ex: Exception) {
            showToast(ex.toString())
        }
    }

    private fun changeMuteButtonState(isMicOn: Boolean) {
        val customMessage = JsonObject()
        customMessage.addProperty("id", vm.getAgoraUid())
        customMessage.addProperty("is_mic_on", isMicOn)
        customMessage.addProperty("action", "MIC_STATUS_CHANGES")
        vm.sendCustomMessage(customMessage, channelName)
    }

    private fun setNotificationStates() {
        /*roomReference?.collection("notifications")?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                for (item in value.documentChanges) {
                    val map = item.document.data
                    val mapTo = map["to"] as HashMap<String, String>
                    if (mapTo["uid"]?.toInt()?.equals(agoraUid) == true) {

                        notificationTo = map["to"] as HashMap<String, String>
                        notificationFrom = map["from"] as HashMap<String, String>
                        notificationType = map["type"].toString()

                        if (isRoomCreatedByUser) {
                            when (notificationType) {
                                "HAND_RAISED" -> {
                                    setNotificationBarFieldsWithActions(
                                        "Dismiss", "Invite to speak", String.format(
                                            "\uD83D\uDC4B %s has something to say. Invite " +
                                                    "them as speakers?",
                                            notificationFrom?.get("name")
                                        )
                                    )
                                }
                                "SPEAKER_INVITE_ACCEPTED" -> {
                                    setNotificationWithoutAction(
                                        String.format(
                                            "%s is now a speaker!",
                                            notificationFrom?.get("name")
                                        ), true
                                    )
                                }

                            }
                        } else {
                            if (notificationType == "SPEAKER_INVITE" && !isInviteRequestComeFromModerator) {
                                setNotificationBarFieldsWithActions(
                                    "Maybe later?", "Join as speaker", String.format(
                                        "\uD83D\uDC4B %s invited you to join as a speaker",
                                        notificationFrom?.get("name")
                                    )
                                )
                            }
                        }
                        roomReference?.collection("notifications")?.document(item.document.id)
                            ?.delete()?.addOnFailureListener {
                                setNotificationWithoutAction("Something Went Wrong", false)
                            }
                    }
                }
            }
        }*/
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
        if (isRoomCreatedByUser) {
            binding.notificationBar.getUserUuid()?.let {
                val customMessage = JsonObject()
                customMessage.addProperty("id", vm.getAgoraUid())
                customMessage.addProperty("uid", it)
                customMessage.addProperty("action", "INVITE_SPEAKER")
                vm.updateInviteSentToUserForSpeaker(it)
                vm.sendCustomMessage(customMessage, it.toString())
            }
            binding.notificationBar.loadAnimationSlideUp()
        }
        else {

            val customMessage = JsonObject()
            customMessage.addProperty("id", vm.getAgoraUid())
            customMessage.addProperty("is_speaker", true)
            customMessage.addProperty("is_mic_on", false)
            customMessage.addProperty("short_name", vm.getCurrentUser()?.name)
            customMessage.addProperty("action", "MOVE_TO_SPEAKER")
            vm.sendCustomMessage(customMessage, channelName)
            isInviteRequestComeFromModerator = true
            binding.notificationBar.loadAnimationSlideUp()
        }

    }

    override fun onRejectNotification() {
        if (isRoomCreatedByUser.not()) {
            val customMessage = JsonObject()
            customMessage.addProperty("id", vm.getAgoraUid())
            customMessage.addProperty("is_hand_raised", false)
            customMessage.addProperty("short_name", vm.getCurrentUser()?.name ?: DEFAULT_NAME)
            customMessage.addProperty("action", "IS_HAND_RAISED")
            vm.sendCustomMessage(customMessage, vm.getModeratorId().toString())
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
                finish()
            }, 4000)
        }
    }

    private fun switchRoles() {
        /*usersReference?.document(agoraUid.toString())?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value?.exists() == false) {
                Log.d("ABC2", "switchRoles() called with: value = $value, error = $error")
                finish()
            }
            if (value != null) {
                val isUserSpeaker = value["is_speaker"]
                val isMicOn = value["is_mic_on"]
                if (!isRoomCreatedByUser && isUserSpeaker?.equals(isRoomUserSpeaker) == false) {
                    if (isUserSpeaker == true) {
                        updateUiWhenSwitchToSpeaker(isMicOn)
                    } else {
                        updateUiWhenSwitchToListener()
                    }
                }
            }
        }*/
    }

    private fun updateUiWhenSwitchToListener() {
        isRoomUserSpeaker = false
        mBoundService?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
        //mBoundService?.muteCall()
        Log.d("Manjul", "updateUiWhenSwitchToListener() called")
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
        setHandRaiseValueToFirestore(false)
        isHandRaised = true
        iSSoundOn = isMicOn == true
        updateMuteButtonState()
        when (iSSoundOn) {
            true -> mBoundService?.unMuteCall()
            false -> mBoundService?.muteCall()
        }
    }

    private fun setHandRaiseValueToFirestore(is_hand_raised: Boolean) {
        /*val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_hand_raised", is_hand_raised)?.addOnFailureListener {
            setNotificationWithoutAction("Something Went Wrong", false)
        }*/
    }

    private fun leaveRoomIfModeratorEndRoom() {


    }

    private fun takePermissions() {
        if (PermissionUtils.isCallingPermissionWithoutLocationEnabled(this)) {
            callWebRtcService()
            return
        }

        PermissionUtils.onlyCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            callWebRtcService()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@ConversationLiveRoomActivity,
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@ConversationLiveRoomActivity)
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
            ReactiveNetwork.observeNetworkConnectivity(applicationContext)
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
            AudienceAdapter(isRoomCreatedByUser)

        binding.speakersRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ConversationLiveRoomActivity, 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = speakerAdapter
        }

        binding.listenerRecyclerView.apply {
            layoutManager = GridLayoutManager(this@ConversationLiveRoomActivity, 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = audienceAdapter
        }

        audienceAdapter?.setOnItemClickListener(object : AudienceAdapter.OnUserItemClickListener {

            override fun onItemClick(user: LiveRoomUser) {
                if (supportFragmentManager.backStackEntryCount == 0 && isBottomSheetVisible.not()) {
                    getDataOnSpeakerAdapterItemClick(user, user.id, false)
                    isBottomSheetVisible = true
                }
            }
        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(user: LiveRoomUser) {
                if (supportFragmentManager.backStackEntryCount == 0 && isBottomSheetVisible.not()) {
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
            isRoomCreatedByUser,
            isRoomUserSpeaker,
            toSpeaker,
            user?.name ?: "",
            user?.photoUrl ?: "",
            userUid == vm.getAgoraUid()
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
                            openUserProfile(mentorId)
                        }
                        else {
                            showToast(getString(R.string.generic_message_for_error))
                        }
                    }

                    override fun moveToAudience() {
                        val customMessage = JsonObject()
                        customMessage.addProperty("id", userUid.toString())
                        customMessage.addProperty("is_speaker", false)
                        customMessage.addProperty("name", userName)
                        customMessage.addProperty("is_mic_on", false)
                        customMessage.addProperty("action", "MOVE_TO_AUDIENCE")
                        vm.sendCustomMessage(customMessage, channelName)

                    }

                    override fun moveToSpeaker() {
                        if (vm.getSpeakerList().size < 16) {
                            val customMessage = JsonObject()
                            customMessage.addProperty("id", vm.getAgoraUid())
                            customMessage.addProperty("uid", userUid)
                            customMessage.addProperty("is_mic_on", false)
                            customMessage.addProperty("action", "INVITE_SPEAKER")
                            vm.updateInviteSentToUserForSpeaker(userUid!!)
                            vm.sendCustomMessage(customMessage, userUid.toString())
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
        bottomSheet.show(supportFragmentManager, "Bottom sheet")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    private fun openUserProfile(mentorId: String) {
        ProfileActivity.openProfileActivity(this, mentorId)
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
                roomId ?: 0, vm.getModeratorId(), vm.getModeratorName(), channelName,
                ArrayList(raisedHandList)
            )
        binding.notificationBar.loadAnimationSlideUp()
        bottomSheet.show(supportFragmentManager, "Bottom sheet Hands Raised")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    override fun onUserInvitedToSpeak(user: LiveRoomUser) {
        vm.updateInviteSentToUser(user.id!!)
        //todo check speaker list size
        val customMessage = JsonObject()
        customMessage.addProperty("id", vm.getAgoraUid())
        customMessage.addProperty("uid", user.id)
        customMessage.addProperty("action", "INVITE_SPEAKER")
        user.id?.let {
            vm.updateInviteSentToUserForSpeaker(user.id!!)
            vm.sendCustomMessage(customMessage, user.id.toString())
        }
        //TODO("Not yet implemented")
    }

    private fun showEndRoomPopup() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
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
                finish()
            }
            mBoundService?.endRoom(roomId, roomQuestionId)
            isExitApiFired = true
            alertDialog.dismiss()
            vm.unSubscribePubNub()
            finish()
        }
    }

    private fun showLeaveRoomPopup() {
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
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
                finish()
            }
            mBoundService?.leaveRoom(roomId, roomQuestionId)
            isExitApiFired = true
            alertDialog.dismiss()
            vm.unSubscribePubNub()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, ConvoWebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
        vm.setReplaySubject(ReplaySubject.create<Any>())
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        unbindService(myConnection)
    }

    override fun onResume() {
        super.onResume()
        compositeDisposable.clear()
        observeNetwork()
        if (vm.isPubNubUsersFetched.value == true) {
            addPubNubEventObserver()
        }
    }

    override fun onBackPressed() {
        isBackPressed = true
        if (!internetAvailableFlag) {
            mBoundService?.endService()
            super.onBackPressed()
            return
        }
        if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
            showEndRoomPopup()
        }
        else {
            showLeaveRoomPopup()
        }
    }

    override fun onDestroy() {
        if ((isBackPressed.or(isExitApiFired)).not()) {
            if (isRoomCreatedByUser) {
                mBoundService?.endRoom(roomId, roomQuestionId)
            }
            else {
                mBoundService?.leaveRoom(roomId, roomQuestionId)
            }
        }
        binding.notificationBar.destroyMediaPlayer()
        stopService(Intent(this, ConvoWebRtcService::class.java))
        super.onDestroy()

    }

    fun getHandRaiseListRefreshListener(): HandRaiseListRefreshListener? {
        return fragmentRefreshListener
    }

    fun setHandRaiseListRefreshListener(fragmentRefreshListener: HandRaiseListRefreshListener?) {
        this.fragmentRefreshListener = fragmentRefreshListener
    }

    private var fragmentRefreshListener: HandRaiseListRefreshListener? = null

    public interface HandRaiseListRefreshListener {
        fun onHandRaiseListRefresh()
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

        fun getIntent(
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
        ) = Intent(context, ConversationLiveRoomActivity::class.java).apply {
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

        fun startRoomActivity(
            activity: Activity,
            channelName: String?,
            uid: Int?,
            token: String?,
            isRoomCreatedByUser: Boolean,
            roomId: Int?,
            roomQuestionId: Int? = null,
            moderatorId: Int? = null,
            topicName: String? = null,
            flags: Array<Int> = arrayOf()
        ) {
            val intent = Intent(activity, ConversationLiveRoomActivity::class.java).apply {
                Log.d("ABC2", "startRoomActivity() called isRoomCreatedByUser ${isRoomCreatedByUser}")
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
            activity.startActivity(intent)
        }

        fun getIntentForNotification(
            context: Context,
            roomId: String,
            topicName: String? = null,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, ConversationLiveRoomActivity::class.java).apply {

            putExtra(OPEN_FROM_NOTIFICATION, true)
            putExtra(ROOM_ID, roomId.toInt())
            putExtra(TOPIC_NAME, topicName)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }
}