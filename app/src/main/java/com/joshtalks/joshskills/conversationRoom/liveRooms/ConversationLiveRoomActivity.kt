package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
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
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.ActivityConversationLiveRoomBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
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
    var listenerAdapter: SpeakerAdapter? = null
    private var engine: RtcEngine? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var moderatorUid: Int? = null
    var iSSoundOn = true
    var isHandRaised = true
    var topicName: String? = null
    var notificationTo: HashMap<String, String>? = null
    var notificationFrom: HashMap<String, String>? = null
    var notificationType: String? = null
    var speakingUsersNewList = arrayListOf<Int>()
    var speakingUsersOldList = arrayListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()
        getIntentExtras()
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        roomReference = liveRoomReference.document(roomId.toString())
        usersReference = roomReference?.collection("users")
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
        binding.userPhoto.setImage(Mentor.getInstance().getUser()?.photo ?: "")
        roomReference?.get()?.addOnSuccessListener {
            moderatorUid = it.get("started_by")?.toString()?.toInt()
            topicName = it.get("topic")?.toString()
            binding.topic.text = topicName
        }
        if (isRoomCreatedByUser) {
            binding.handRaiseBtn.visibility = View.GONE
            binding.raisedHands.visibility = View.VISIBLE
            engine?.muteLocalAudioStream(false)
        }
    }

    private fun clickListener() {

        binding.leaveEndRoomBtn.setOnClickListener {
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
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

        binding.raisedHands.setOnClickListener {
            openRaisedHandsBottomSheet()
        }
    }

    private fun clickHandRaisedButton(isRaised: Boolean, type: String) {
        val reference = usersReference?.document(agoraUid.toString())
        reference?.update("is_hand_raised", isRaised)
            ?.addOnSuccessListener {
                isHandRaised = !isHandRaised
                when (isRaised) {
                    true -> binding.handRaiseBtn.text = getString(R.string.raised)
                    false -> binding.handRaiseBtn.text = getString(R.string.unraised)
                }
                sendNotification(
                    type,
                    agoraUid?.toString(),
                    moderatorUid?.toString()
                )
            }?.addOnFailureListener {
                showApiCallErrorToast(it.message ?: "")
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
                    true -> binding.muteBtn.text = getString(R.string.mute)
                    false -> binding.muteBtn.text = getString(R.string.unmute)
                }

            }?.addOnFailureListener {
                showApiCallErrorToast(it.message ?: "")
            }
    }

    private fun sendNotification(type: String, fromUid: String?, toUiD: String?) {
        roomReference?.collection("notifications")?.document()?.set(
            hashMapOf(
                "from" to hashMapOf(
                    "uid" to fromUid,
                    "name" to "listener name"
                ),
                "to" to hashMapOf(
                    "uid" to toUiD,
                    "name" to "Moderator"
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
                            if (notificationType == "HAND_RAISED") {
                                setNotificationBarFields(
                                    "Dismiss", "Invite to speak", String.format(
                                        "\uD83D\uDC4B %s has something to say. Invite" +
                                                "them as speakers?", notificationFrom?.get("name")
                                    )
                                )
                            } else {
                                binding.notificationBar.visibility = View.GONE
                            }
                        } else {
                            if (notificationType == "SPEAKER_INVITE") {
                                setNotificationBarFields(
                                    "Maybe later?", "Join as speaker", String.format(
                                        "\uD83D\uDC4B %s invited you to join as a speaker",
                                        notificationFrom?.get("name")
                                    )
                                )
                            }
                        }
                        roomReference?.collection("notifications")?.document(item.document.id)
                            ?.delete()
                    } else {
                        binding.notificationBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setNotificationBarFields(
        rejectedText: String,
        acceptedText: String,
        heading: String
    ) {
        binding.notificationBar.visibility = View.VISIBLE
        binding.notificationBar.setRejectButtonText(rejectedText)
        binding.notificationBar.setAcceptButtonText(acceptedText)
        binding.notificationBar.setHeading(heading)
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
                } /*else {
                    binding.muteBtn.visibility = View.VISIBLE
                    binding.handRaiseBtn.visibility = View.GONE
                    binding.muteBtn.text = getString(R.string.mute)
                    iSSoundOn = true
                    engine?.enableLocalAudio(true)

                }*/
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
        binding.handRaiseBtn.text = getString(R.string.unraised)
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
            binding.muteBtn.text = getString(R.string.mute)
        } else {
            binding.muteBtn.text = getString(R.string.unmute)
        }
    }

    private fun initializeEngine() {
        if (engine == null) {
            engine = AppObjectController.getRtcEngine(AppObjectController.joshApplication)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "On new intent engine: $engine")
    }

    override fun onRestart() {
        super.onRestart()
        if (engine == null) {
            Log.d(TAG, "onRestart: engine null")
        } else {
            Log.d(TAG, "onRestart: engine not null")
        }
        Log.d(TAG, "onRestart:  engine : $engine & channelNmae: $channelName")

    }


    private fun leaveRoomIfModeratorEndRoom() {
        roomReference?.addSnapshotListener { value, error ->
            if (error != null) {
                Log.d(TAG, error.message)
                return@addSnapshotListener
            } else {
                if (value?.exists() == false) {
                    engine?.leaveChannel()
                    finish()
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
            Log.d(TAG, "onLeaveChannel: ")
            engine = null
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
            Log.d(TAG, "onRejoinChannelSuccess: ")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            if (isRoomCreatedByUser) {
                if (reason == Constants.USER_OFFLINE_QUIT || reason == Constants.USER_OFFLINE_DROPPED) {
                    usersReference?.document(uid.toString())?.delete()
                }
            }
            if (uid == moderatorUid) {
                usersReference?.addSnapshotListener { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (value != null) {
                        val secondUserId = value.documents[1].id.toInt()
                        if (agoraUid == secondUserId) {
                            viewModel.leaveEndRoom(true, roomId)
                        }
                    }
                }
            }
            Log.d(TAG, "onUserOffline: ")
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
            SpeakerAdapter(getFirestoreRecyclerOptions(false), this, isRoomCreatedByUser)
        binding.speakersList.layoutManager = GridLayoutManager(this, 3)
        binding.listenerList.layoutManager = GridLayoutManager(this, 4)
        binding.speakersList.setHasFixedSize(false)
        binding.listenerList.setHasFixedSize(false)
        binding.speakersList.adapter = speakerAdapter
        binding.listenerList.adapter = listenerAdapter

        listenerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
                getDataOnSpeakerAdapterItemClick(documentSnapshot, false)
            }
        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
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
        showBottomSheet(roomInfo, liveRoomUser?.mentor_id ?: "", userUid)
    }

    private fun getFirestoreRecyclerOptions(isSpeaker: Boolean): FirestoreRecyclerOptions<LiveRoomUser> {
        val query = usersReference?.whereEqualTo("is_speaker", isSpeaker)
        return FirestoreRecyclerOptions.Builder<LiveRoomUser>()
            .setQuery(query!!, LiveRoomUser::class.java)
            .build()
    }

    private fun showBottomSheet(
        roomInfo: ConversationRoomBottomSheetInfo,
        mentorId: String,
        userUid: Int?
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
                        val reference = usersReference?.document(userUid.toString())
                        reference?.update("is_speaker", true)
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

    override fun onStart() {
        super.onStart()
        speakerAdapter?.startListening()
        listenerAdapter?.startListening()
        if (ConversationRoomListingActivity.CONVERSATION_ROOM_VISIBLE_TRACK_FLAG)
            viewModel.makeEnterExitConversationRoom(true)
    }

    override fun onStop() {
        super.onStop()
        speakerAdapter?.stopListening()
        listenerAdapter?.stopListening()
        if (ConversationRoomListingActivity.CONVERSATION_ROOM_VISIBLE_TRACK_FLAG)
            viewModel.makeEnterExitConversationRoom(false)
    }

    override fun onResume() {
        super.onResume()
        ConversationRoomListingActivity.CONVERSATION_ROOM_VISIBLE_TRACK_FLAG = true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
    }

    override fun onDestroy() {
        viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
        if (engine != null) {
            engine?.leaveChannel()
            engine = null
            Log.d(TAG, "Agora engine set null")
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
                notificationFrom?.get("uid")
            )
            binding.notificationBar.visibility = View.GONE
        } else {
            if (notificationType == "SPEAKER_INVITE" && notificationTo?.get("uid").toString()
                    .toInt() == agoraUid
            ) {
                val reference = usersReference?.document(agoraUid.toString())
                reference?.update("is_speaker", true)?.addOnSuccessListener {
                    binding.notificationBar.visibility = View.GONE
                    sendNotification(
                        "SPEAKER_INVITE_ACCEPTED",
                        notificationTo?.get("uid"),
                        notificationFrom?.get("uid")
                    )
                }
            }
        }

    }

    override fun onRejectNotification() {
        binding.notificationBar.visibility = View.GONE
    }
}