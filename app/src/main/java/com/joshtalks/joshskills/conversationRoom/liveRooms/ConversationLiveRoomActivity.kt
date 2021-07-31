package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheet
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetAction
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetInfo
import com.joshtalks.joshskills.conversationRoom.bottomsheet.RaisedHandsBottomSheet
import com.joshtalks.joshskills.conversationRoom.notification.NotificationView
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingActivity
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setUserImageRectOrInitials
import com.joshtalks.joshskills.databinding.ActivityConversationLiveRoomBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.voip.ConversationRoomCallback
import com.joshtalks.joshskills.ui.voip.ConversationRoomJoin
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE
import io.agora.rtc.IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ConversationLiveRoomActivity : BaseActivity(), ConversationLiveRoomSpeakerClickAction,
    NotificationView.NotificationViewAction {
    private var mServiceBound: Boolean = false
    lateinit var binding: ActivityConversationLiveRoomBinding
    lateinit var viewModel: ConversationLiveRoomViewModel
    val database = FirebaseFirestore.getInstance()
    var roomReference: DocumentReference? = null
    var usersReference: CollectionReference? = null
    private var mBoundService: WebRtcService? = null

    var roomId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    var isRoomUserSpeaker: Boolean = false
    var speakerAdapter: SpeakerAdapter? = null
    var listenerAdapter: AudienceAdapter? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var moderatorUid: Int? = null
    var moderatorName: String? = null
    var moderatorMentorId: String? = null
    var currentUserName: String? = null
    var iSSoundOn = true
    var isBottomSheetVisible = false
    var isHandRaised = true
    var topicName: String? = null
    var notificationTo: HashMap<String, String>? = null
    var notificationFrom: HashMap<String, String>? = null
    var notificationType: String? = null
    var speakingUsersNewList = arrayListOf<Int>()
    var speakingUsersOldList = arrayListOf<Int>()
    var handler: Handler? = null
    var runnable: Runnable? = null
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true
    private var isInviteRequestComeFromModerator: Boolean = false
    var isBackPressed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.window.statusBarColor =
                this.resources.getColor(R.color.conversation_room_color, theme)
        }
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isBackPressed = false
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()
        getIntentExtras()
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        val liveRoomReference = database.collection("conversation_rooms")
        roomReference = liveRoomReference.document(roomId.toString())
        usersReference = roomReference?.collection("users")
        getUserName()
        handler = Handler(Looper.getMainLooper())
        updateUI()
        setNotificationStates()
        leaveRoomIfModeratorEndRoom()

        viewModel.navigation.observe(this, {
            when (it) {
                is ConversationLiveRoomNavigation.ApiCallError -> showApiCallErrorToast("Something went wrong. Please try Again!!!")
                is ConversationLiveRoomNavigation.ExitRoom -> finish()
            }
        })

        clickListener()
        switchRoles()
        speakerAdapter?.startListening()
        listenerAdapter?.startListening()
        takePermissions()
    }

    private fun callWebRtcService() {
        val intent = Intent(AppObjectController.joshApplication, WebRtcService::class.java)
        intent.action = ConversationRoomJoin().action
        intent.putExtra("token", token)
        intent.putExtra("channel_name", channelName)
        intent.putExtra("uid", agoraUid)
        intent.putExtra("isModerator", isRoomCreatedByUser)
        startService(intent)
        WebRtcService.isConversionRoomActive = true
        WebRtcService.moderatorUid = moderatorUid
        WebRtcService.agoraUid = agoraUid
        WebRtcService.roomId = roomId?.toString()
        WebRtcService.isRoomCreatedByUser = isRoomCreatedByUser
    }

    private var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(conversationRoomCallback)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mServiceBound = false
        }

    }

    private fun getUserName() {
        usersReference?.document(agoraUid.toString())?.get()?.addOnSuccessListener {
            currentUserName = it.get("name").toString()
            iSSoundOn = it.get("is_mic_on") == true
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
        channelName = intent?.getStringExtra("CHANNEL_NAME")
        agoraUid = intent?.getIntExtra("UID", 0)
        token = intent?.getStringExtra("TOKEN")
        roomId = intent?.getIntExtra("ROOM_ID", 0)
        isRoomCreatedByUser = intent.getBooleanExtra("IS_ROOM_CREATED_BY_USER", false)
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
        roomReference?.get()?.addOnSuccessListener {
            moderatorUid = it.get("started_by")?.toString()?.toInt()
            topicName = it.get("topic")?.toString()
            binding.topic.text = topicName
            usersReference?.document(moderatorUid.toString())?.get()
                ?.addOnSuccessListener { moderator ->
                    moderatorName = moderator.get("name")?.toString()
                    moderatorMentorId = moderator.get("mentor_id")?.toString()
                }
        }

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
                viewModel.leaveEndRoom(isRoomCreatedByUser, roomId, moderatorMentorId)
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
        val reference = usersReference?.document(agoraUid.toString())
        usersReference?.whereEqualTo("is_speaker", true)
            ?.get()
            ?.addOnSuccessListener { documents ->
                if (documents.size() < 16) {
                    reference?.update("is_hand_raised", isRaised)
                        ?.addOnSuccessListener {
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
                            sendNotification(
                                type,
                                agoraUid?.toString(),
                                currentUserName ?: "User",
                                moderatorUid?.toString(),
                                moderatorName ?: "Moderator"
                            )
                        }?.addOnFailureListener {
                            setNotificationWithoutAction("Something Went Wrong", false)
                        }
                } else {
                    setNotificationWithoutAction(
                        "Room has reached maximum allowed number of speakers." +
                                " Please try again after sometime.", false
                    )
                }
            }
            ?.addOnFailureListener { exception ->
                setNotificationWithoutAction("Something Went Wrong", false)
            }

    }

    private fun changeMuteButtonState(isMicOn: Boolean) {
        val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_mic_on", isMicOn)
            ?.addOnSuccessListener {
                iSSoundOn = isMicOn
                updateMuteButtonState()
            }?.addOnFailureListener {
                setNotificationWithoutAction("Something Went Wrong", false)
            }
    }

    private fun sendNotification(
        type: String,
        fromUid: String?,
        fromName: String,
        toUiD: String?,
        toName: String
    ) {
        roomReference?.collection("notifications")?.document()?.set(
            hashMapOf(
                "from" to hashMapOf(
                    "uid" to fromUid,
                    "name" to fromName
                ),
                "to" to hashMapOf(
                    "uid" to toUiD,
                    "name" to toName
                ),
                "type" to type
            )
        )?.addOnFailureListener {
            setNotificationWithoutAction("Something Went Wrong", false)
        }
    }

    private fun setNotificationStates() {
        roomReference?.collection("notifications")?.addSnapshotListener { value, error ->
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
        }
    }

    private fun setNotificationBarFieldsWithActions(
        rejectedText: String,
        acceptedText: String,
        heading: String,
    ) {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            showActionLayout()
            setRejectButtonText(rejectedText)
            setAcceptButtonText(acceptedText)
            setHeading(heading)
            startSound()
            setBackgroundColor(true)
            loadAnimationSlideDown()
        }
        if (runnable != null) {
            handler?.removeCallbacks(runnable)
        }
    }

    private fun hideNotificationAfter4seconds() {
        if (runnable == null) {
            setRunnable()
            handler?.postDelayed(runnable, 4000)
        } else {
            handler?.removeCallbacks(runnable)
            setRunnable()
            handler?.postDelayed(runnable, 4000)
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

    private fun showRoomEndNotification() {
        binding.notificationBar.apply {
            visibility = View.VISIBLE
            hideActionLayout()
            setBackgroundColor(false)
            setHeading("This room has ended")
            startSound()
            loadAnimationSlideDown()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 4000)
    }

    private fun switchRoles() {
        usersReference?.document(agoraUid.toString())?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value?.exists() == false) {
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
        }
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
        val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_hand_raised", is_hand_raised)?.addOnFailureListener {
            setNotificationWithoutAction("Something Went Wrong", false)
        }
    }

    private fun leaveRoomIfModeratorEndRoom() {
        roomReference?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (value?.exists() == false) {
                    mBoundService?.leaveChannel()
                    showRoomEndNotification()
                }
            }
        }
        usersReference?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            } else {
                if (value?.isEmpty == true) {
                    mBoundService?.leaveChannel()
                    showRoomEndNotification()
                }
            }
        }

    }

    private fun takePermissions() {
        if (PermissionUtils.isDemoCallingPermissionEnabled(this)) {
            callWebRtcService()
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
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

    private var conversationRoomCallback = object : ConversationRoomCallback {

        override fun onUserOffline(uid: Int, reason: Int) {
            val isUserLeave = reason == Constants.USER_OFFLINE_QUIT
            if (isRoomCreatedByUser) {
                if (isUserLeave) {
                    usersReference?.document(uid.toString())?.delete()
                }
            } else {
                if (uid == moderatorUid && isUserLeave) {
                    usersReference?.get()?.addOnSuccessListener { documents ->
                        if (documents.size() > 1) {
                            if (documents.documents[0].id.toInt() == agoraUid) {
                                viewModel.leaveEndRoom(true, roomId, moderatorMentorId)
                            } else if (documents.documents[1].id.toInt() == agoraUid) {
                                viewModel.leaveEndRoom(true, roomId, moderatorMentorId)
                            }
                        }

                    }
                }
            }
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            if (isRoomCreatedByUser) {
                speakingUsersOldList.clear()
                speakingUsersOldList.addAll(speakingUsersNewList)
                speakingUsersNewList.clear()
                speakers?.forEach {
                    if (it.uid != 0 && it.volume > 0) {
                        speakingUsersNewList.add(it.uid)
                    }
                    if (it.uid == 0 && it.volume > 0) {
                        speakingUsersNewList.add(agoraUid ?: 0)
                    }
                }
                updateFirestoreData()
            }
        }

        override fun onSwitchToSpeaker() {
            mBoundService?.setClientRole(CLIENT_ROLE_BROADCASTER)
        }

        override fun onSwitchToAudience() {
            mBoundService?.setClientRole(CLIENT_ROLE_AUDIENCE)
        }

    }

    private fun updateFirestoreData() {
        speakingUsersOldList.forEach {
            usersReference?.document(it.toString())?.update("is_speaking", false)
                ?.addOnFailureListener {
                    setNotificationWithoutAction("Something Went Wrong", false)
                }
        }
        speakingUsersNewList.forEach {
            usersReference?.document(it.toString())?.update("is_speaking", true)
                ?.addOnFailureListener {
                    setNotificationWithoutAction("Something Went Wrong", false)
                }
        }

    }

    private fun showApiCallErrorToast(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
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
            hideActionLayout()
            setHeading("The Internet connection appears to be offline")
            setBackgroundColor(false)
        }

    }

    private fun internetAvailable() {
        binding.notificationBar.apply {
            visibility = View.GONE
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
            SpeakerAdapter(getFirestoreRecyclerOptions(true), this, isRoomCreatedByUser)
        listenerAdapter =
            AudienceAdapter(getFirestoreRecyclerOptions(false), this, isRoomCreatedByUser)
        binding.speakersList.apply {
            layoutManager = GridLayoutManager(this@ConversationLiveRoomActivity, 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = speakerAdapter
        }

        binding.listenerList.apply {
            layoutManager = GridLayoutManager(this@ConversationLiveRoomActivity, 3)
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            itemAnimator = null
            adapter = listenerAdapter
        }

        listenerAdapter?.setOnItemClickListener(object : AudienceAdapter.OnUserItemClickListener {

            override fun onItemClick(user: LiveRoomUser, userUid: Int) {
                if (supportFragmentManager.backStackEntryCount == 0 && isBottomSheetVisible.not()) {
                    getDataOnSpeakerAdapterItemClick(user, userUid, false)
                    isBottomSheetVisible = true
                }
            }
        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(user: LiveRoomUser, userUid: Int) {
                if (supportFragmentManager.backStackEntryCount == 0 && isBottomSheetVisible.not()) {
                    getDataOnSpeakerAdapterItemClick(user, userUid, true)
                    isBottomSheetVisible = true
                }
            }
        })

    }

    private fun getDataOnSpeakerAdapterItemClick(
        user: LiveRoomUser?, userUid: Int,
        toSpeaker: Boolean
    ) {
        val roomInfo = ConversationRoomBottomSheetInfo(
            isRoomCreatedByUser,
            isRoomUserSpeaker,
            toSpeaker,
            user?.name ?: "",
            user?.photo_url ?: "",
            userUid == agoraUid
        )
        usersReference?.document(userUid.toString())?.get()?.addOnSuccessListener {
            showBottomSheet(
                roomInfo,
                user?.mentor_id ?: "",
                userUid,
                it.get("name").toString()
            )
        }
    }

    private fun getFirestoreRecyclerOptions(isSpeaker: Boolean): FirestoreRecyclerOptions<LiveRoomUser> {
        val query = usersReference?.whereEqualTo("is_speaker", isSpeaker)?.orderBy("sort_order")
        return FirestoreRecyclerOptions.Builder<LiveRoomUser>()
            .setQuery(query!!, LiveRoomUser::class.java)
            .build()
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
                        openUserProfile(mentorId)
                    }

                    override fun moveToAudience() {
                        val reference = usersReference?.document(userUid.toString())
                        reference?.update("is_speaker", false)?.addOnFailureListener {
                            setNotificationWithoutAction("Something Went Wrong", false)
                        }
                    }

                    override fun moveToSpeaker() {
                        usersReference?.whereEqualTo("is_speaker", true)
                            ?.get()
                            ?.addOnSuccessListener { documents ->
                                if (documents.size() < 16) {
                                    sendNotification(
                                        "SPEAKER_INVITE",
                                        moderatorUid?.toString(),
                                        moderatorName ?: "Moderator",
                                        userUid.toString(),
                                        userName
                                    )
                                } else {
                                    setNotificationWithoutAction(
                                        "Room has reached maximum allowed number of speakers." +
                                                " Please try again after sometime.", false
                                    )
                                }
                            }?.addOnFailureListener {
                                setNotificationWithoutAction("Something Went Wrong", false)
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
        val bottomSheet =
            RaisedHandsBottomSheet.newInstance(roomId ?: 0, moderatorUid, moderatorName)
        bottomSheet.show(supportFragmentManager, "Bottom sheet Hands Raised")
        bottomSheet.dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId, moderatorMentorId)
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
    }

    override fun onPause() {
        super.onPause()
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
        ConversationRoomListingActivity.CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = true
    }

    override fun onBackPressed() {
        isBackPressed = true
        if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
            showEndRoomPopup()
        } else {
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId, moderatorMentorId)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        speakerAdapter?.stopListening()
        listenerAdapter?.stopListening()
        if (!isBackPressed) {
            if (isRoomCreatedByUser) {
                mBoundService?.endRoom(roomId?.toString(), moderatorUid)
            } else {
                mBoundService?.leaveRoom(roomId?.toString(), moderatorUid)
            }
        }
        binding.notificationBar.destroyMediaPlayer()
        super.onDestroy()

    }

    companion object {
        private const val TAG = "ConversationLiveRoomAct"
    }

    override fun onAcceptNotification() {
        if (isRoomCreatedByUser) {
            sendNotification(
                "SPEAKER_INVITE",
                moderatorUid?.toString(),
                moderatorName ?: "Moderator",
                notificationFrom?.get("uid").toString(),
                notificationFrom?.get("name").toString()
            )
            binding.notificationBar.loadAnimationSlideUp()
        } else {
            if (notificationType == "SPEAKER_INVITE" && notificationTo?.get("uid").toString()
                    .toInt() == agoraUid
            ) {
                val reference = usersReference?.document(agoraUid.toString())
                reference?.update("is_speaker", true)?.addOnSuccessListener {
                    sendNotification(
                        "SPEAKER_INVITE_ACCEPTED",
                        notificationTo?.get("uid"),
                        notificationTo?.get("name").toString(),
                        notificationFrom?.get("uid"),
                        notificationFrom?.get("name").toString()
                    )
                    isInviteRequestComeFromModerator = true
                    binding.notificationBar.loadAnimationSlideUp()
                }?.addOnFailureListener {
                    binding.notificationBar.loadAnimationSlideUp()
                    isInviteRequestComeFromModerator = false
                    usersReference?.document(agoraUid.toString())
                        ?.update("is_speaker_invite_sent", false)?.addOnFailureListener {
                            setNotificationWithoutAction("Something Went Wrong", false)
                        }
                }
            }
        }

    }

    override fun onRejectNotification() {
        if (notificationType == "SPEAKER_INVITE" && notificationTo?.get("uid").toString()
                .toInt() == agoraUid
        ) {
            isInviteRequestComeFromModerator = false
            usersReference?.document(agoraUid.toString())?.update("is_speaker_invite_sent", false)
                ?.addOnFailureListener {
                    setNotificationWithoutAction("Something Went Wrong", false)
                }
        }
        binding.notificationBar.loadAnimationSlideUp()
    }
}