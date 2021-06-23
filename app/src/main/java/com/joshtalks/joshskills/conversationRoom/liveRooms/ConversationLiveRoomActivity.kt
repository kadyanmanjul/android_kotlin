package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.abs

class ConversationLiveRoomActivity : BaseActivity(), ConversationLiveRoomSpeakerClickAction,
    NotificationView.NotificationViewAction {
    lateinit var binding: ActivityConversationLiveRoomBinding
    lateinit var viewModel: ConversationLiveRoomViewModel
    val liveRoomReference = FirebaseFirestore.getInstance().collection("conversation_rooms")
    var roomReference: DocumentReference? = null
    var usersReference: CollectionReference? = null

    var roomId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    var isRoomUserSpeaker: Boolean = false
    var speakerAdapter: SpeakerAdapter? = null
    var listenerAdapter: AudienceAdapter? = null
    private var engine: RtcEngine? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var moderatorUid: Int? = null
    var moderatorName: String? = null
    var moderatorMentorId: String? = null
    var currentUserName: String? = null
    var iSSoundOn = true
    var isHandRaised = true
    var topicName: String? = null
    var notificationTo: HashMap<String, String>? = null
    var notificationFrom: HashMap<String, String>? = null
    var notificationType: String? = null
    var speakingUsersNewList = arrayListOf<Int>()
    var speakingUsersOldList = arrayListOf<Int>()
    var handler: Handler? = null
    var runnable: Runnable? = null
    var micBlackDrawable: Drawable? = null
    var micOffDrawable: Drawable? = null
    var handRaisedDrawable: Drawable? = null
    var handUnRaisedDrawable: Drawable? = null
    private val compositeDisposable = CompositeDisposable()
    private var internetAvailableFlag: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()
        getIntentExtras()
        setImageDrawable()
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        roomReference = liveRoomReference.document(roomId.toString())
        usersReference = roomReference?.collection("users")
        getUserName()
        handler = Handler(Looper.getMainLooper())
        updateUI()
        initializeEngine()
        takePermissions()
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
    }

    private fun setImageDrawable() {
        micBlackDrawable = ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_mic_black_on,
            null
        )
        micOffDrawable = ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_mic_off,
            null
        )
        handRaisedDrawable = ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_hand_raised_icon,
            null
        )
        handUnRaisedDrawable = ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_unraise_hand_icon,
            null
        )
    }

    private fun getUserName() {
        usersReference?.document(agoraUid.toString())?.get()?.addOnSuccessListener {
            currentUserName = it.get("name").toString()
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
            binding.muteBtn.visibility = View.VISIBLE
            engine?.muteLocalAudioStream(false)
        } else {
            binding.handRaiseBtn.visibility = View.VISIBLE
            binding.raisedHands.visibility = View.GONE
            binding.muteBtn.visibility = View.GONE
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

        binding.handRaiseBtn.setOnClickListener {
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
                                    binding.handRaiseBtn.setImageDrawable(
                                        handRaisedDrawable
                                    )
                                    setNotificationWithoutAction(
                                        String.format(
                                            "\uD83D\uDC4B You raised your hand! We’ll let the speakers\n" +
                                                    "know you want to talk..."
                                        ), true
                                    )
                                }
                                false -> binding.handRaiseBtn.setImageDrawable(
                                    handUnRaisedDrawable
                                )
                            }
                            sendNotification(
                                type,
                                agoraUid?.toString(),
                                currentUserName ?: "User",
                                moderatorUid?.toString(),
                                moderatorName ?: "Moderator"
                            )
                        }?.addOnFailureListener {
                            showApiCallErrorToast(it.message ?: "")
                        }
                } else {
                    setNotificationWithoutAction(
                        "Room has reached maximum allowed number of speakers." +
                                " Please try again after sometime.", false
                    )
                }
            }
            ?.addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

    }

    private fun changeMuteButtonState(isMicOn: Boolean) {
        val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_mic_on", isMicOn)
            ?.addOnSuccessListener {
                iSSoundOn = isMicOn
                engine?.enableLocalAudio(iSSoundOn)
                engine?.muteLocalAudioStream(!iSSoundOn)
                when (isMicOn) {
                    true -> {
                        binding.muteBtn.setImageDrawable(
                            micBlackDrawable
                        )
                    }
                    false -> {
                        binding.muteBtn.setImageDrawable(
                            micOffDrawable
                        )
                    }
                }

            }?.addOnFailureListener {
                showApiCallErrorToast(it.message ?: "")
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
        )
    }

    private fun setNotificationStates() {
        roomReference?.collection("notifications")?.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                for (item in value.documentChanges) {
                    val map = item.document.data
                    notificationTo = map["to"] as HashMap<String, String>
                    notificationFrom = map["from"] as HashMap<String, String>
                    notificationType = map["type"].toString()

                    if (notificationTo?.get("uid")?.toInt()?.equals(agoraUid) == true) {
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
                            if (notificationType == "SPEAKER_INVITE") {
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
                                Log.d(TAG, "notification not deleted")
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
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.showActionLayout()
        binding.notificationBar.setRejectButtonText(rejectedText)
        binding.notificationBar.setAcceptButtonText(acceptedText)
        binding.notificationBar.setHeading(heading)
        binding.notificationBar.setBackgroundColor(true)
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
            binding.notificationBar.visibility = View.GONE
        }
    }


    private fun setNotificationWithoutAction(heading: String, isGreenColorNotification: Boolean) {
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.hideActionLayout()
        binding.notificationBar.setHeading(heading)
        binding.notificationBar.setBackgroundColor(isGreenColorNotification)
        hideNotificationAfter4seconds()
    }

    private fun showRoomEndNotification() {
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.hideActionLayout()
        binding.notificationBar.setBackgroundColor(false)
        binding.notificationBar.setHeading("This room has ended")
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
        initializeEngine()
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
        binding.muteBtn.visibility = View.GONE
        binding.handRaiseBtn.visibility = View.VISIBLE
    }

    private fun updateUiWhenSwitchToSpeaker(isMicOn: Any?) {
        isRoomUserSpeaker = true
        initializeEngine()
        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        binding.muteBtn.visibility = View.VISIBLE
        binding.handRaiseBtn.visibility = View.GONE
        setHandRaiseValueToFirestore(false)
        binding.handRaiseBtn.setImageDrawable(
            handUnRaisedDrawable
        )
        isHandRaised = true
        iSSoundOn = isMicOn == true
        engine?.enableLocalAudio(iSSoundOn)
        engine?.muteLocalAudioStream(!iSSoundOn)
        updateMuteButtonText()
    }

    private fun setHandRaiseValueToFirestore(is_hand_raised: Boolean) {
        val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_hand_raised", is_hand_raised)
    }

    private fun updateMuteButtonText() {
        if (iSSoundOn) {
            binding.muteBtn.setImageDrawable(
                micBlackDrawable
            )
        } else {
            binding.muteBtn.setImageDrawable(
                micOffDrawable
            )
        }
    }

    private fun initializeEngine() {
        if (engine == null) {
            engine = AppObjectController.getRtcEngine(AppObjectController.joshApplication)
        }
    }

    private fun leaveRoomIfModeratorEndRoom() {
        roomReference?.addSnapshotListener { value, error ->
            if (error != null) {
                Log.d(TAG, error.message)
                return@addSnapshotListener
            } else {
                if (value?.exists() == false) {
                    engine?.leaveChannel()
                    showRoomEndNotification()
                }
            }
        }
        usersReference?.addSnapshotListener { value, error ->
            if (error != null) {
                Log.d(TAG, error.message)
                return@addSnapshotListener
            } else {
                if (value?.isEmpty == true) {
                    engine?.leaveChannel()
                    showRoomEndNotification()
                }
            }
        }

    }

    private fun takePermissions() {
        if (PermissionUtils.isDemoCallingPermissionEnabled(this)) {
            joinChannel(channelName)
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            joinChannel(channelName)
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

    private fun joinChannel(channelName: String?) {

        engine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        if (isRoomCreatedByUser) {
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
        } else {
            engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
        }
        if (eventListener != null) {
            engine?.removeHandler(eventListener)
        }
        if (eventListener != null) {
            engine?.addHandler(eventListener)
        }

        engine?.enableAudioVolumeIndication(2000, 3, true)
        engine?.setAudioProfile(
            Constants.AUDIO_PROFILE_SPEECH_STANDARD,
            Constants.AUDIO_SCENARIO_GAME_STREAMING
        )

        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        val res = engine?.joinChannel(
            token, channelName, "test", agoraUid!!, option
        )
        if (res != 0) {
            showAlert(res?.let { abs(it) }?.let { RtcEngine.getErrorDescription(it) })
            return
        }
    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            agoraUid = uid
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            if (isRoomCreatedByUser) {
                if (reason == Constants.USER_OFFLINE_QUIT || reason == Constants.USER_OFFLINE_DROPPED) {
                    usersReference?.document(uid.toString())?.delete()
                }
            } else {
                if (uid == moderatorUid && reason == Constants.USER_OFFLINE_DROPPED) {
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
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            super.onAudioVolumeIndication(speakers, totalVolume)
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

    }

    private fun updateFirestoreData() {
        speakingUsersOldList.forEach {
            usersReference?.document(it.toString())?.update("is_speaking", false)
        }
        speakingUsersNewList.forEach {
            usersReference?.document(it.toString())?.update("is_speaking", true)
        }

    }

    private fun showApiCallErrorToast(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(message: String?) {
        AlertDialog.Builder(this).setTitle("Tips").setMessage(message)
            .setPositiveButton("OK") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .show()
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
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.hideActionLayout()
        binding.notificationBar.setHeading("The Internet connection appears to be offline")
        binding.notificationBar.setBackgroundColor(false)
    }

    private fun internetAvailable() {
        binding.notificationBar.visibility = View.GONE
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
        speakerAdapter?.notifyDataSetChanged()
        listenerAdapter?.notifyDataSetChanged()
        speakerAdapter =
            SpeakerAdapter(getFirestoreRecyclerOptions(true), this, isRoomCreatedByUser)
        listenerAdapter =
            AudienceAdapter(getFirestoreRecyclerOptions(false), this, isRoomCreatedByUser)
        binding.speakersList.layoutManager = GridLayoutManager(this, 3)
        binding.listenerList.layoutManager = GridLayoutManager(this, 4)
        binding.speakersList.setHasFixedSize(false)
        binding.listenerList.setHasFixedSize(false)
        binding.speakersList.adapter = speakerAdapter
        binding.listenerList.adapter = listenerAdapter

        listenerAdapter?.setOnItemClickListener(object : AudienceAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
                if (supportFragmentManager.backStackEntryCount == 0)
                getDataOnSpeakerAdapterItemClick(documentSnapshot, false)
            }
        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
                if (supportFragmentManager.backStackEntryCount == 0)
                    getDataOnSpeakerAdapterItemClick(documentSnapshot, true)
            }
        })

    }

    private fun getDataOnSpeakerAdapterItemClick(
        documentSnapshot: DocumentSnapshot?,
        toSpeaker: Boolean
    ) {
        val userUid = documentSnapshot?.id?.toInt()
        val liveRoomUser = documentSnapshot?.toObject(LiveRoomUser::class.java)
        val roomInfo = ConversationRoomBottomSheetInfo(
            isRoomCreatedByUser,
            isRoomUserSpeaker,
            toSpeaker,
            liveRoomUser?.name ?: "",
            liveRoomUser?.photo_url ?: "",
            userUid == agoraUid
        )
        usersReference?.document(userUid.toString())?.get()?.addOnSuccessListener {
            showBottomSheet(
                roomInfo,
                liveRoomUser?.mentor_id ?: "",
                userUid,
                it.get("name").toString()
            )
        }
    }

    private fun getFirestoreRecyclerOptions(isSpeaker: Boolean): FirestoreRecyclerOptions<LiveRoomUser> {
        val query = usersReference?.whereEqualTo("is_speaker", isSpeaker)
        query?.orderBy("sort_order", Query.Direction.ASCENDING)
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
                        reference?.update("is_speaker", false)
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
                            }
                    }

                })
        bottomSheet.show(supportFragmentManager, "Bottom sheet")

    }

    private fun openUserProfile(mentorId: String) {
        UserProfileActivity.startUserProfileActivity(
            this@ConversationLiveRoomActivity,
            mentorId,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), isFromConversationRoom = true
        )
    }

    fun openRaisedHandsBottomSheet() {
        val bottomSheet = RaisedHandsBottomSheet.newInstance(roomId ?: 0, moderatorUid)
        bottomSheet.show(supportFragmentManager, "Bottom sheet Hands Raised")
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
        speakerAdapter?.startListening()
        listenerAdapter?.startListening()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()
        speakerAdapter?.stopListening()
        listenerAdapter?.stopListening()
        compositeDisposable.clear()
    }

    override fun onResume() {
        super.onResume()
        observeNetwork()
        ConversationRoomListingActivity.CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = true
    }

    override fun onBackPressed() {
        if (binding.leaveEndRoomBtn.text == getString(R.string.end_room)) {
            showEndRoomPopup()
        } else {
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId, moderatorMentorId)
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (engine != null) {
            engine?.leaveChannel()
            engine = null
        }
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
            binding.notificationBar.visibility = View.GONE
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
                }
                binding.notificationBar.visibility = View.GONE
            }
        }

    }

    override fun onRejectNotification() {
        binding.notificationBar.visibility = View.GONE
    }
}