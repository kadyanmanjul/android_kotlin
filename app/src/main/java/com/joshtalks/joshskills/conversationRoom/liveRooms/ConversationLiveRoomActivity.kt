package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.animation.ObjectAnimator
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheet
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetAction
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetInfo
import com.joshtalks.joshskills.conversationRoom.bottomsheet.RaisedHandsBottomSheet
import com.joshtalks.joshskills.conversationRoom.model.LiveRoomUser
import com.joshtalks.joshskills.conversationRoom.model.RoomListResponseItem
import com.joshtalks.joshskills.conversationRoom.notification.NotificationView
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingNavigation
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingViewModel
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.databinding.ActivityConversationLiveRoomBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.*
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.presence.PNGetStateResult
import com.pubnub.api.models.consumer.presence.PNHereNowResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.IRtcEngineEventHandler.AudioVolumeInfo
import io.agora.rtc.IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE
import io.agora.rtc.IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class ConversationLiveRoomActivity : BaseActivity(), ConversationLiveRoomSpeakerClickAction,
    NotificationView.NotificationViewAction, RaisedHandsBottomSheet.HandRaiseSheetListener {

    private var pubnub: PubNub? = null
    private var timer: CountDownTimer? = null
    private var timeCreated: Int = 0
    private var mServiceBound: Boolean = false
    private lateinit var binding: ActivityConversationLiveRoomBinding
    private var mBoundService: WebRtcService? = null
    private var isActivityOpenFromNotification: Boolean = false
    private var roomId: Int? = null
    private var roomQuestionId: Int? = null
    private var isRoomCreatedByUser: Boolean = false
    private var isRoomUserSpeaker: Boolean = false
    private var speakerAdapter: SpeakerAdapter? = null
    private var audienceAdapter: AudienceAdapter? = null
    private var channelName: String? = null
    private var agoraUid: Int? = null
    private var token: String? = null
    private var moderatorUid: Int? = null
    private var moderatorName: String? = null
    private var iSSoundOn = true
    private var isBottomSheetVisible = false
    private var isHandRaised = true
    private var notificationTo: HashMap<String, String>? = null
    private var notificationFrom: HashMap<String, String>? = null
    private var notificationType: String? = null
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isInviteRequestComeFromModerator: Boolean = false
    private var isBackPressed: Boolean = false
    private val viewModel by lazy { ViewModelProvider(this).get(ConversationRoomListingViewModel::class.java) }
    private var currentUser: LiveRoomUser? = null
    val speakersList: ArrayList<LiveRoomUser> = arrayListOf()
    val audienceList: ArrayList<LiveRoomUser> = arrayListOf()
    val speakingListForGoldenRing: ArrayList<Int?> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, theme)
        }
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isBackPressed = false
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, true)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isActivityOpenFromNotification =
            intent?.getBooleanExtra(OPEN_FROM_NOTIFICATION, false) == true

        if (isActivityOpenFromNotification) {
            addObservers()
            getIntentExtras(intent)
        } else {
            getIntentExtras()
            initData()
            if (isRoomCreatedByUser) {
                initSearchingView()
                startProgressBarCountDown()
            }
        }
        initPubNub()
    }

    private fun initSearchingView() {
        binding.searchingContainer.visibility = View.VISIBLE
        binding.progressBar.max = 100
        binding.progressBar.progress = 0

        viewModel.isRoomEnded.observe(this, {
            if (it == true) {
                finish()
            }
        })

    }

    private fun startProgressBarCountDown() {
        runOnUiThread {
            binding.progressBar.max = 100
            binding.progressBar.progress = 0
            timer = object : CountDownTimer(5000, 500) {
                override fun onTick(millisUntilFinished: Long) {
                    timeCreated = timeCreated + 1
                    if (timeCreated >= 60 * 2 * 2) {
                        endRoom()
                    }
                    val diff = binding.progressBar.progress + 10
                    fillProgressBar(diff)
                }

                override fun onFinish() {
                    startProgressBarCountDown()
                }
            }
            timer?.start()
        }
    }

    private fun endRoom() {
        Timber.tag("ABC").e("endRoom() called")
        timer?.cancel()
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
        viewModel.endRoom(roomId.toString(), roomQuestionId)
    }

    private fun leaveRoom() {
        mBoundService?.leaveRoom(roomId?.toString(), roomQuestionId)
        PrefManager.put(PREF_IS_CONVERSATION_ROOM_ACTIVE, false)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS, true)
        finish()
    }

    private fun fillProgressBar(diff: Int) {
        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(
                binding.progressBar,
                "progress",
                binding.progressBar.progress,
                diff
            )
        animation.startDelay = 0
        animation.duration = 250
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.start()
    }

    private fun initPubNub() {
        val pnConf = PNConfiguration()
        pnConf.subscribeKey = BuildConfig.PUBNUB_SUB_API_KEY
        pnConf.publishKey = BuildConfig.PUBNUB_PUB_API_KEY
        pnConf.uuid = Mentor.getInstance().getId()
        pnConf.isSecure = false
        pubnub = PubNub(pnConf)

        pubnub?.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                Log.d(
                    "ABC",
                    "message() called with: pubnub = $pubnub, pnMessageResult = $pnMessageResult"
                )
                val msg = pnMessageResult.message.asJsonObject
                val act = msg["action"].asString

                if (msg != null) {
                    when (act) {
                        "CREATE_ROOM" -> addNewUserToAudience(msg)
                        "JOIN_ROOM" -> addNewUserToAudience(msg)
                        "LEAVE_ROOM" -> removeUser(msg)
                        "END_ROOM" -> leaveRoom()
                        "IS_HAND_RAISED" -> handRaisedByUser(msg)
                        "INVITE_SPEAKER" -> inviteUserToSpeaker()
                        "MOVE_TO_SPEAKER" -> moveToSpeaker(msg)
                        "MOVE_TO_AUDIENCE" -> moveToAudience(msg)
                        "MIC_STATUS_CHANGES" -> changeMicStatus(msg)
                        else -> {

                        }
                    }
                }
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

            override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}

            override fun channel(
                pubnub: PubNub,
                pnChannelMetadataResult: PNChannelMetadataResult
            ) {
            }

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

            override fun messageAction(
                pubnub: PubNub,
                pnMessageActionResult: PNMessageActionResult
            ) {
            }

            override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
        })

        getLatestUserList()
    }

    private fun handRaisedByUser(msg: JsonObject) {
        if (msg.get("is_hand_raised").asBoolean) {
            setNotificationBarFieldsWithActions(
                "Dismiss", "Invite to speak", String.format(
                    "\uD83D\uDC4B %s has something to say. Invite " +
                            "them as speakers?",
                    msg.get("name")
                ), msg.get("id").asInt
            )
            setHandRaisedForUser(msg.get("id").asInt, true)
        } else {
            setHandRaisedForUser(msg.get("id").asInt, false)
        }
    }

    private fun setHandRaisedForUser(userId: Int, isHandRaised: Boolean) {
        viewModel.updateHandRaisedToUser(userId, isHandRaised)
        CoroutineScope(Dispatchers.Main).launch {
            audienceAdapter?.updateHandRaisedViaId(userId, isHandRaised)
        }
    }

    private fun moveToSpeaker(msg: JsonObject) {
        if (moderatorUid == agoraUid) {
            CoroutineScope(Dispatchers.Main).launch {
                setNotificationWithoutAction(
                    String.format(
                        "%s is now a speaker!",
                        msg.get("name").asString
                    ), true
                )
            }
        }

        msg.get("id").asInt?.let { agoraId ->
            CoroutineScope(Dispatchers.Main).launch {
                moveToSpeaker(agoraId)
            }
        }
    }

    private fun moveToAudience(msg: JsonObject) {
        msg.get("id").asInt?.let { agoraId ->
            CoroutineScope(Dispatchers.Main).launch {
                moveToAudience(agoraId)
            }
        }
    }

    private fun changeMicStatus(eventObject: JsonObject) {
        Log.d("ABC", "presence() called mic_status_changes")
        if (agoraUid == eventObject.get("id").asInt) {
            iSSoundOn = eventObject.get("is_mic_on").asBoolean
            setPresenceStateForUuid(currentUser, iSSoundOn)
            CoroutineScope(Dispatchers.Main).launch {
                updateMuteButtonState()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            speakerAdapter?.updateMicViaId(
                eventObject.get("id").asInt,
                eventObject.get("is_mic_on").asBoolean
            )

        }
    }

    private fun inviteUserToSpeaker() {
        setNotificationBarFieldsWithActions(
            "Maybe later?", "Join as speaker", String.format(
                "\uD83D\uDC4B %s invited you to join as a speaker",
                moderatorName
            ), moderatorUid
        )

    }

    private fun moveToSpeaker(agoraId: Int) {
        val user = audienceList.filter { it.id == agoraId }
        user.forEach {
            if (this.agoraUid == it.id) {
                updateUiWhenSwitchToSpeaker(it.isMicOn)
            }
            audienceList.remove(it)
            it.isSpeaker = true
            it.isHandRaised = false
            it.isInviteSent = true
            speakersList.add(it)
            setPresenceStateForUuid(it)
        }
        audienceAdapter?.updateFullList(audienceList)
        viewModel.updateAudienceList(audienceList)
        speakerAdapter?.updateFullList(speakersList)
    }

    private fun moveToAudience(agoraId: Int) {
        val user = speakersList.filter { it.id == agoraId }
        user.forEach {
            if (this.agoraUid == it.id) {
                updateUiWhenSwitchToListener()
            }
            speakersList.remove(it)
            it.isSpeaker = false
            it.isHandRaised = false
            it.isInviteSent = false
            audienceList.add(it)
            setPresenceStateForUuid(it)
        }
        audienceAdapter?.updateFullList(audienceList)
        viewModel.updateAudienceList(audienceList)
        speakerAdapter?.updateFullList(speakersList)
    }

    private fun getPresenceStateFromUuid(uuid: String) {
        pubnub?.presenceState
            ?.uuid(uuid)
            ?.channels(Arrays.asList(channelName))
            ?.async(object : PNCallback<PNGetStateResult> {
                override fun onResponse(result: PNGetStateResult?, status: PNStatus) {
                    if (status.isError.not()) {

                    }
                }
            })
    }

    private fun setPresenceStateForUuid(user: LiveRoomUser?, isMicOn: Boolean? = null) {
        if (user == null || pubnub == null) {
            return
        }
        val state = JsonObject()
        state.addProperty("is_speaker", user.isSpeaker)
        state.addProperty("name", user.name)
        state.addProperty("photo_url", user.photoUrl)
        state.addProperty("sort_order", user.sortOrder)
        state.addProperty("is_moderator", user.isModerator)
        state.addProperty("is_mic_on", isMicOn ?: user.isMicOn)
        state.addProperty("is_speaking", user.isSpeaking)
        state.addProperty("is_hand_raised", user.isHandRaised)
        state.addProperty("mentor_id", user.mentorId)

        pubnub!!.setPresenceState()
            .channels(Arrays.asList(channelName))
            .state(state)
            .uuid(user.id.toString())
            .async { result, status ->
            }
    }

    private fun getLatestUserList() {
        pubnub?.hereNow()
            ?.channels(Arrays.asList(channelName))
            ?.includeUUIDs(true)
            ?.includeState(true)
            ?.async(object : PNCallback<PNHereNowResult> {
                override fun onResponse(result: PNHereNowResult?, status: PNStatus) {
                    result?.channels?.get(channelName)?.occupants?.forEach {
                        refreshUsersList(it.state)
                    }
                    /*result?.channels?.forEach { t, u ->
                        Log.d("ABC", "here now   t = $t, u = $u")
                        u.occupants.forEach {
                            //getAllUsersData(it.state)
                            Log.d("ABC", "occupants onResponse() called $it")
                        }
                    }*/
                }

            })
    }

    private fun refreshUsersList(state: JsonElement?) {
        if (state == null) {
            return
        }
        Log.d("ABC", "refreshUsersList() called with: state = $state")
        val user = getAllUsersData(state)
        if (user.isModerator) {
            if (moderatorUid == null || moderatorUid == 0) {
                moderatorUid = user.id
            }
            moderatorName = user.name
        }
        if (user.id == agoraUid) {
            currentUser = user
        }
        if (user.isSpeaker == true) {
            speakersList.add(user)
        } else {
            audienceList.add(user)
        }
        speakerAdapter?.updateFullList(speakersList)
        audienceAdapter?.updateFullList(audienceList)
        viewModel.updateAudienceList(audienceList)
    }

    private fun getAllUsersData(msgJson: JsonElement): LiveRoomUser {
        val data = msgJson.asJsonObject
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        return AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
    }

    private fun addNewUserToAudience(msg: JsonObject) {
        val data = msg["data"].asJsonObject
        val matType = object : TypeToken<LiveRoomUser>() {}.type
        if (data == null) {
            return
        }
        val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
        CoroutineScope(Dispatchers.Main).launch {
            if (user.isModerator) {
                moderatorUid = user.id
                moderatorName = user.name
            }
            if (user.id == agoraUid) {
                currentUser = user
            }
            if (user.isSpeaker == true) {
                speakersList.add(user)
                speakerAdapter?.updateFullList(speakersList)
            } else {
                if (binding.searchingContainer.visibility == View.VISIBLE) {
                    timer?.cancel()
                    timer = null
                    binding.searchingContainer.visibility = View.GONE
                }
                audienceList.add(user)
                audienceAdapter?.updateFullList(audienceList)
                viewModel.updateAudienceList(audienceList)
            }
        }
    }

    /* private fun addNewUserToSpeaker(msg: JsonObject) {
         val data = msg["data"].asJsonObject
         val matType = object : TypeToken<LiveRoomUser>() {}.type
         if (data == null) {
             return
         }
         val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
         CoroutineScope(Dispatchers.Main).launch {
             if (user.isSpeaker == true){
                 speakerAdapter?.addSingleItem(user)
             } else {
                 audienceAdapter?.addSingleItem(user)
             }
         }
     }*/

    private fun removeUser(msg: JsonObject) {
        val data: JsonObject? = msg["data"].asJsonObject
        data?.let {
            Log.d(TAG, "removeUser() called ${data.get("id")}")
            val matType = object : TypeToken<LiveRoomUser>() {}.type
            if (data == null) {
                return
            }
            val user = AppObjectController.gsonMapper.fromJson<LiveRoomUser>(data, matType)
            CoroutineScope(Dispatchers.Main).launch {
                if (user?.isSpeaker == true) {
                    val list = speakersList.filter { it.id == user.id }
                    speakersList.removeAll(list)
                    speakersList.sortBy { it.sortOrder }
                    speakerAdapter?.updateFullList(speakersList)
                } else {

                    val list = audienceList.filter { it.id == user.id }
                    audienceList.removeAll(list)
                    audienceList.sortBy { it.sortOrder }
                    audienceAdapter?.updateFullList(audienceList)
                    viewModel.updateAudienceList(audienceList)
//                    audienceAdapter?.removeSingleItem(user)
                }
            }
        }
    }

    private fun initData() {
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        //TODO init data to adapters
        if (isRoomCreatedByUser) {
            updateMuteButtonState()
        } else {
            binding.apply {
                muteBtn.visibility = View.GONE
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
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
            return
        }
    }

    private fun addObservers() {
        showProgressBar()
        viewModel.navigation.observe(this, {
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
        })
    }

    private fun setValues(
        channelName: String?,
        uid: Int?,
        token: String?,
        roomCreatedByUser: Boolean,
        roomId: Int?
    ) {
        this.channelName = channelName
        this.agoraUid = uid
        this.token = token
        this.roomId = roomId
        this.roomQuestionId = null
        this.isRoomCreatedByUser = roomCreatedByUser
        initData()
        hideProgressBar()
    }

    private fun showApiCallErrorToast(error: String) {
        hideProgressBar()
        if (error.isNotEmpty()) {
            binding.notificationBar.apply {
                visibility = View.VISIBLE
                hideActionLayout()
                setHeading(error)
                setBackgroundColor(false)
                loadAnimationSlideDown()
                startSound()
                hideNotificationAfter4seconds()
            }
        } else {
            Toast.makeText(this, "Something Went Wrong !!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getIntentExtras(intent: Intent?) {
        roomId = intent?.getIntExtra(ROOM_ID, 0)
        if (isActivityOpenFromNotification && roomId != null) {
            viewModel.joinRoom(
                RoomListResponseItem(roomId.toString(),
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
        val intent = Intent(AppObjectController.joshApplication, WebRtcService::class.java)
        intent.action = ConversationRoomJoin().action
        intent.putExtra("token", token)
        intent.putExtra("channel_name", channelName)
        intent.putExtra("uid", agoraUid)
        intent.putExtra("isModerator", isRoomCreatedByUser)
        WebRtcService.isConversionRoomActive = true
        AppObjectController.joshApplication.startService(intent)
        WebRtcService.isConversionRoomActive = true
        WebRtcService.moderatorUid = moderatorUid
        WebRtcService.agoraUid = agoraUid
        WebRtcService.roomId = roomId?.toString()
        WebRtcService.roomQuestionId = roomQuestionId
        WebRtcService.isRoomCreatedByUser = if (moderatorUid != null) {
            moderatorUid == agoraUid
        } else isRoomCreatedByUser

        if (isRoomCreatedByUser) {
            WebRtcService.moderatorUid = agoraUid
            WebRtcService.isRoomCreatedByUser = true

        }
        removeIncomingNotification()
    }

    private fun removeIncomingNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(9999)
    }

    private var callback: ConversationRoomCallback = object : ConversationRoomCallback {
        override fun onUserOffline(uid: Int, reason: Int) {

        }

        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            val uids = Arrays.stream(speakers)
                .map { s: AudioVolumeInfo -> if (s.uid == null || s.uid == 0) agoraUid else s.uid }
                .collect(Collectors.toList())
            refreshSpeakingUsers(uids)
        }

        override fun onSwitchToSpeaker() {
            //TODO("Not yet implemented")
        }

        override fun onSwitchToAudience() {
            //TODO("Not yet implemented")
        }

    }

    private fun refreshSpeakingUsers(uids: List<Int?>) {
        Log.d("ABC", "refreshSpeakingUsers() called with: uids = $uids")
        speakingListForGoldenRing.clear()
        speakingListForGoldenRing.addAll(uids)
        val i = 0
        for (speaker in speakersList) {
            val viewHolder = binding.speakersRecyclerView.findViewHolderForAdapterPosition(i)
            if (viewHolder is SpeakerAdapter.SpeakerViewHolder) {
                viewHolder.setGoldenRingVisibility(speakingListForGoldenRing.contains(speaker.id))
            }
        }
    }

    private var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callback)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mServiceBound = false
        }

    }

    private fun updateMuteButtonState() {
        when (iSSoundOn) {
            true -> {
                binding.unmuteBtn.visibility = View.VISIBLE
                binding.muteBtn.visibility = View.GONE
                mBoundService?.unMuteCall()
            }
            false -> {
                binding.unmuteBtn.visibility = View.GONE
                binding.muteBtn.visibility = View.VISIBLE
                mBoundService?.muteCall()
            }
        }
    }

    private fun getIntentExtras() {
        channelName = intent?.getStringExtra(CHANNEL_NAME)
        agoraUid = intent?.getIntExtra(UID, 0)
        moderatorUid = intent?.getIntExtra(MODERATOR_UID, 0)
        token = intent?.getStringExtra(TOKEN)
        roomId = intent?.getIntExtra(ROOM_ID, 0)
        roomQuestionId = intent?.getIntExtra(ROOM_QUESTION_ID, 0)
        isRoomCreatedByUser = intent.getBooleanExtra(IS_ROOM_CREATED_BY_USER, false)
    }

    private fun updateUI() {
        setUpRecyclerView()
        setLeaveEndButton(isRoomCreatedByUser)
        binding.userPhoto.clipToOutline = true
        binding.userPhoto.setUserImageRectOrInitials(
            Mentor.getInstance().getUser()?.photo,
            User.getInstance().firstName ?: "JS", 16, true, 8,
            textColor = R.color.black,
            bgColor = R.color.conversation_room_gray
        )
        /*roomReference?.get()?.addOnSuccessListener {
            moderatorUid = it.get("started_by")?.toString()?.toInt()
            WebRtcService.moderatorUid = moderatorUid
            WebRtcService.isRoomCreatedByUser = moderatorUid == agoraUid
            Log.d("ABC", "moderatorUid set")
            topicName = it.get("topic")?.toString()
            WebRtcService.conversationRoomTopicName = topicName
            binding.topic.text = topicName
            usersReference?.document(moderatorUid.toString())?.get()
                ?.addOnSuccessListener { moderator ->
                    moderatorName = moderator.get("name")?.toString()
                    moderatorMentorId = moderator.get("mentor_id")?.toString()
                }
        }*/

        if (isRoomCreatedByUser) {
            binding.handRaiseBtn.visibility = View.GONE
            binding.raisedHands.visibility = View.VISIBLE
            mBoundService?.setClientRole(CLIENT_ROLE_BROADCASTER)
        } else {
            binding.handRaiseBtn.visibility = View.VISIBLE
            binding.raisedHands.visibility = View.GONE
            mBoundService?.setClientRole(CLIENT_ROLE_AUDIENCE)
        }
    }

    private fun clickListener() {

        binding.leaveEndRoomBtn.setOnSingleClickListener {
            if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
                showEndRoomPopup()
            } else {
                mBoundService?.leaveRoom(roomId?.toString(), roomQuestionId)
            }
        }

        binding.userPhoto.setOnClickListener {
            openUserProfile(Mentor.getInstance().getId())
        }
        binding.muteBtn.setOnClickListener {
            when (iSSoundOn) {
                true -> changeMuteButtonState(false)
                false -> changeMuteButtonState(true)
            }
        }
        binding.unmuteBtn.setOnClickListener {
            when (iSSoundOn) {
                true -> changeMuteButtonState(false)
                false -> changeMuteButtonState(true)
            }
        }

        binding.handRaiseBtn.setOnClickListener {
            when (isHandRaised) {
                true -> clickHandRaisedButton(true, "HAND_RAISED")
                false -> clickHandRaisedButton(false, "HAND_UNRAISED")
            }
        }
        binding.handUnraiseBtn.setOnClickListener {
            when (isHandRaised) {
                true -> clickHandRaisedButton(true, "HAND_RAISED")
                false -> clickHandRaisedButton(false, "HAND_UNRAISED")
            }
        }

        binding.raisedHands.setOnSingleClickListener {
            openRaisedHandsBottomSheet()
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
                        ), true
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
            customMessage.addProperty("id", agoraUid)
            customMessage.addProperty("is_hand_raised", isRaised)
            customMessage.addProperty("name", currentUser?.name ?: DEFAULT_NAME)
            customMessage.addProperty("action", "IS_HAND_RAISED")
            sendCustomMessage(customMessage, moderatorUid.toString())

        } catch (ex: Exception) {
            showToast(ex.toString())
        }
    }

    private fun sendCustomMessage(state: JsonElement, channelName: String? = null) {
        channelName?.let {
            /*pubnub!!.setPresenceState()
                .channels(Arrays.asList(channelName))
                .state(state)
                .async { result, status ->
                    Log.d("ABC", "setPresenceState() called with: result = $result, status = $status")
                }*/

            pubnub?.publish()
                ?.message(state)
                ?.channel(channelName)
                ?.async(object : PNCallback<PNPublishResult> {
                    override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                        if (status.isError.not()) {
                            Log.d(
                                "ABC",
                                "onResponse() called with: state = $state, channelName = $channelName result = $result"
                            )
                        }
                    }
                })
        }
    }

    private fun changeMuteButtonState(isMicOn: Boolean) {
        val customMessage = JsonObject()
        customMessage.addProperty("id", agoraUid)
        customMessage.addProperty("is_mic_on", isMicOn)
        customMessage.addProperty("action", "MIC_STATUS_CHANGES")
        sendCustomMessage(customMessage, channelName)
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
        userUid: Int?
    ) {
        CoroutineScope(Dispatchers.Main).launch {

            binding.notificationBar.apply {
                visibility = View.VISIBLE
                showActionLayout()
                setRejectButtonText(rejectedText)
                setAcceptButtonText(acceptedText)
                setUserUuid(userUid)
                setHeading(heading)
                startSound()
                setBackgroundColor(true)
                loadAnimationSlideDown()
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
        } else {
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


    private fun setNotificationWithoutAction(heading: String, isGreenColorNotification: Boolean) {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
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
                customMessage.addProperty("id", agoraUid)
                customMessage.addProperty("uid", it)
                customMessage.addProperty("action", "INVITE_SPEAKER")
                sendCustomMessage(customMessage, it.toString())
            }
            binding.notificationBar.loadAnimationSlideUp()
        } else {

            val customMessage = JsonObject()
            customMessage.addProperty("id", agoraUid)
            customMessage.addProperty("is_speaker", true)
            customMessage.addProperty("name", currentUser?.name)
            customMessage.addProperty("action", "MOVE_TO_SPEAKER")
            sendCustomMessage(customMessage, channelName)
            isInviteRequestComeFromModerator = true
            binding.notificationBar.loadAnimationSlideUp()
        }

    }

    override fun onRejectNotification() {
        if (isRoomCreatedByUser.not()) {
            val customMessage = JsonObject()
            customMessage.addProperty("id", agoraUid)
            customMessage.addProperty("is_hand_raised", false)
            customMessage.addProperty("name", currentUser?.name ?: DEFAULT_NAME)
            customMessage.addProperty("action", "IS_HAND_RAISED")
            sendCustomMessage(customMessage, moderatorUid.toString())
            binding.notificationBar.loadAnimationSlideUp()
        }
        binding.notificationBar.loadAnimationSlideUp()
    }

    private fun showRoomEndNotification(roomId: Int?) {
        Log.d("ABC", "showRoomEndNotification() called with: roomId = $roomId")
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
                Log.d("ABC", "switchRoles() called with: value = $value, error = $error")
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
        mBoundService?.setClientRole(CLIENT_ROLE_AUDIENCE)
        binding.apply {
            muteBtn.visibility = View.GONE
            unmuteBtn.visibility = View.GONE
            handUnraiseBtn.visibility = View.VISIBLE
            handRaiseBtn.visibility = View.GONE
        }
        isInviteRequestComeFromModerator = false
    }

    private fun updateUiWhenSwitchToSpeaker(isMicOn: Any?) {
        isRoomUserSpeaker = true
        isInviteRequestComeFromModerator = true
        mBoundService?.setClientRole(CLIENT_ROLE_BROADCASTER)
        binding.handRaiseBtn.visibility = View.GONE
        binding.handUnraiseBtn.visibility = View.GONE
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
                    permissions: MutableList<PermissionRequest>?,
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
                    internetAvailableFlag = connectivity.available()
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                }
        )
    }

    private fun internetNotAvailable() {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            setHeading("The Internet connection appears to be offline")
            loadAnimationSlideDown()
            startSound()
            hideActionLayout()
            setBackgroundColor(false)
        }
    }

    private fun internetAvailable() {
        binding.notificationBar.apply {
            visibility = View.GONE
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
            SpeakerAdapter(this, isRoomCreatedByUser)
        audienceAdapter =
            AudienceAdapter(this, isRoomCreatedByUser)

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
            userUid == agoraUid
        )
        showBottomSheet(
            roomInfo,
            user?.mentorId ?: EMPTY,
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
                        } else {
                            showToast(getString(R.string.generic_message_for_error))
                        }
                    }

                    override fun moveToAudience() {
                        val customMessage = JsonObject()
                        customMessage.addProperty("id", userUid.toString())
                        customMessage.addProperty("is_speaker", false)
                        customMessage.addProperty("name", userName)
                        customMessage.addProperty("action", "MOVE_TO_AUDIENCE")
                        sendCustomMessage(customMessage, channelName)

                    }

                    override fun moveToSpeaker() {
                        if (speakersList.size < 16) {
                            val customMessage = JsonObject()
                            customMessage.addProperty("id", agoraUid)
                            customMessage.addProperty("uid", userUid)
                            customMessage.addProperty("action", "INVITE_SPEAKER")
                            sendCustomMessage(customMessage, userUid.toString())
                        } else {
                            setNotificationWithoutAction(
                                "Room has reached maximum allowed number of speakers." +
                                        " Please try again after sometime.", false
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
        UserProfileActivity.startUserProfileActivity(
            this@ConversationLiveRoomActivity,
            mentorId,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), isFromConversationRoom = true
        )
    }

    fun openRaisedHandsBottomSheet() {
        val raisedHandList = audienceList.filter { it.isSpeaker == false && it.isHandRaised }
        val bottomSheet =
            RaisedHandsBottomSheet.newInstance(
                roomId ?: 0, moderatorUid, moderatorName, channelName,
                ArrayList(raisedHandList)
            )
        bottomSheet.show(supportFragmentManager, "Bottom sheet Hands Raised")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    }

    override fun onUserInvitedToSpeak(user: LiveRoomUser) {
        viewModel.updateInviteSentToUser(user.id!!)
        val customMessage = JsonObject()
        customMessage.addProperty("id", agoraUid)
        customMessage.addProperty("uid", user.id)
        customMessage.addProperty("action", "INVITE_SPEAKER")
        sendCustomMessage(customMessage, user.id.toString())
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
            alertDialog.dismiss()
        }

        dialogView.findViewById<AppCompatTextView>(R.id.end_room).setOnClickListener {
            Log.d("ABC", "activity showEndRoomPopup() called")
            mBoundService?.endRoom(roomId?.toString(), roomQuestionId)
            alertDialog.dismiss()
            finish()
        }

    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, WebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )

        pubnub?.subscribe()?.channels(
            Arrays.asList(channelName, agoraUid.toString())
        )?.withPresence()
            ?.execute()
    }

    override fun onPause() {
        super.onPause()
        pubnub?.unsubscribeAll()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        unbindService(myConnection)
    }

    override fun onResume() {
        super.onResume()
        observeNetwork()
    }

    override fun onBackPressed() {
        isBackPressed = true
        if (timer != null) {
            endRoom()
            return
        }
        if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
            showEndRoomPopup()
        } else {
            mBoundService?.leaveRoom(roomId?.toString(), roomQuestionId)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        Log.d("ABC", "activity onDestroy() called")
        if (!isBackPressed) {
            if (isRoomCreatedByUser) {
                mBoundService?.endRoom(roomId?.toString(), roomQuestionId)
            } else {
                mBoundService?.leaveRoom(roomId?.toString(), roomQuestionId)
            }
        }
        binding.notificationBar.destroyMediaPlayer()
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

        fun getIntent(
            context: Context,
            channelName: String?,
            uid: Int?,
            token: String?,
            isRoomCreatedByUser: Boolean,
            roomId: Int?,
            roomQuestionId: Int? = null,
            moderatorId: Int? = null,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, ConversationLiveRoomActivity::class.java).apply {

            putExtra(CHANNEL_NAME, channelName)
            putExtra(UID, uid)
            putExtra(MODERATOR_UID, moderatorId)
            putExtra(TOKEN, token)
            putExtra(IS_ROOM_CREATED_BY_USER, isRoomCreatedByUser)
            putExtra(ROOM_ID, roomId)
            putExtra(ROOM_QUESTION_ID, roomQuestionId)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }

        fun getIntentForNotification(
            context: Context,
            roomId: String,
            flags: Array<Int> = arrayOf()
        ) = Intent(context, ConversationLiveRoomActivity::class.java).apply {

            putExtra(OPEN_FROM_NOTIFICATION, true)
            putExtra(ROOM_ID, roomId.toInt())
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }
}