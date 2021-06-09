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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheet
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetAction
import com.joshtalks.joshskills.conversationRoom.bottomsheet.ConversationRoomBottomSheetInfo
import com.joshtalks.joshskills.conversationRoom.bottomsheet.RaisedHandsBottomSheet
import com.joshtalks.joshskills.conversationRoom.notification.NotificationView
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
    val firebaseFirestore = FirebaseFirestore.getInstance()
    var roomId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    var isRoomUserSpeaker: Boolean = false
    var speakerAdapter: SpeakerAdapter? = null
    var listenerAdapter: SpeakerAdapter? = null
    lateinit var notebookRef: CollectionReference
    private var engine: RtcEngine? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var moderatorUid: Int? = null
    var iSSoundOn = true
    var isHandRaised = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()
        binding.notificationBar.setNotificationViewEnquiryAction(this)
        channelName = intent?.getStringExtra("CHANNEL_NAME")
        agoraUid = intent?.getIntExtra("UID", 0)
        token = intent?.getStringExtra("TOKEN")
        roomId = intent?.getIntExtra("ROOM_ID", 0)
        isRoomCreatedByUser = intent.getBooleanExtra("IS_ROOM_CREATED_BY_USER", false)
        if (isRoomCreatedByUser) {
            binding.handRaiseBtn.visibility = View.GONE
            binding.raisedHands.visibility = View.VISIBLE
        }

        setNotificationStates()

        notebookRef = firebaseFirestore.collection("conversation_rooms").document(roomId.toString())
            .collection("users")
        setUpRecyclerView()
        setLeaveEndButton(isRoomCreatedByUser)
        if (engine == null) {
            engine = AppObjectController.getRtcEngine(this)
        }
        leaveRoomIfModeratorEndRoom()
        takePermissions()
        binding.leaveEndRoomBtn.setOnClickListener {
            viewModel.leaveEndRoom(isRoomCreatedByUser, roomId)
        }
        viewModel.navigation.observe(this, {
            when (it) {
                is ConversationLiveRoomNavigation.ApiCallError -> showApiCallErrorToast()
                is ConversationLiveRoomNavigation.ExitRoom -> finish()
            }
        })

        binding.muteBtn.setOnClickListener {
            if (iSSoundOn) {
                val reference = notebookRef.document(agoraUid.toString())
                reference.update("is_mic_on", false)
                    .addOnSuccessListener {
                        iSSoundOn = false
                        engine?.enableLocalAudio(iSSoundOn)
                        binding.muteBtn.text = getString(R.string.unmute)
                    }

            } else {
                val reference = notebookRef.document(agoraUid.toString())

                reference.update("is_mic_on", true)
                    .addOnSuccessListener {
                        iSSoundOn = true
                        engine?.enableLocalAudio(iSSoundOn)
                        binding.muteBtn.text = getString(R.string.mute)
                    }
            }
        }

        binding.handRaiseBtn.setOnClickListener {
            if (isHandRaised) {
                val reference = notebookRef.document(agoraUid.toString())
                reference.update("is_hand_raised", true)
                    .addOnSuccessListener {
                        isHandRaised = !isHandRaised
                        binding.handRaiseBtn.text = getString(R.string.raised)
                    }

            } else {
                val reference = notebookRef.document(agoraUid.toString())

                reference.update("is_hand_raised", false)
                    .addOnSuccessListener {
                        isHandRaised = !isHandRaised
                        binding.handRaiseBtn.text = getString(R.string.unraised)
                    }
            }
        }

        binding.raisedHands.setOnClickListener {
            openRaisedHandsBottomSheet()
        }
        binding.userPhoto.setImage(Mentor.getInstance().getUser()?.photo ?: "")
        binding.userPhoto.setOnClickListener {
            UserProfileActivity.startUserProfileActivity(
                this@ConversationLiveRoomActivity,
                Mentor.getInstance().getId(),
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
        switchRoles()

    }

    private fun setNotificationStates() {

    }

    private fun switchRoles() {
        notebookRef.document(agoraUid.toString()).addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                val isUserSpeaker = value["is_speaker"]
                val isMicOn = value["is_mic_on"]
                if (!isRoomCreatedByUser) {
                    if (isUserSpeaker == true) {
                        isRoomUserSpeaker = true
                        if (engine == null) {
                            engine = AppObjectController.getRtcEngine(this)
                        }
                        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
                        binding.muteBtn.visibility = View.VISIBLE
                        binding.handRaiseBtn.visibility = View.GONE
                        val reference = notebookRef.document(agoraUid.toString())
                        reference.update("is_hand_raised", false)
                        binding.handRaiseBtn.text = getString(R.string.unraised)
                        isHandRaised = true
                        iSSoundOn = isMicOn == true
                        engine?.enableLocalAudio(iSSoundOn)
                        engine?.adjustPlaybackSignalVolume(160)
                        if (iSSoundOn) {
                            binding.muteBtn.text = "Mute"
                        } else {
                            binding.muteBtn.text = "UnMute"
                        }
                    } else {
                        isRoomUserSpeaker = false
                        if (engine == null) {
                            engine = AppObjectController.getRtcEngine(this)
                        }
                        engine?.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
                        binding.muteBtn.visibility = View.GONE
                        binding.handRaiseBtn.visibility = View.VISIBLE
                    }
                } else {
                    binding.muteBtn.visibility = View.VISIBLE
                    binding.handRaiseBtn.visibility = View.GONE
                    binding.muteBtn.text = "Mute"
                    iSSoundOn = true
                    engine?.enableLocalAudio(true)

                }
            }
        }
    }

    private fun leaveRoomIfModeratorEndRoom() {
        notebookRef.addSnapshotListener { value, error ->
            val userList = arrayListOf<ConversationLiveRoomUser>()
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                for (item: DocumentSnapshot in value) {
                    val liveRoomUser = item.toObject(ConversationLiveRoomUser::class.java)
                    if (liveRoomUser != null) {
                        userList.add(liveRoomUser)
                    }
                }
                if (userList.isEmpty()) {
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

        engine?.enableAudioVolumeIndication(1000, 3, true)

        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
//        option.autoSubscribeVideo = true
        val res = engine!!.joinChannel(
            token, channelName, "test", agoraUid!!, option
        )
        if (res != 0) {
            showAlert(RtcEngine.getErrorDescription(abs(res)))
            return
        }
    }

    @Volatile
    private var eventListener: IRtcEngineEventHandler? = object : IRtcEngineEventHandler() {

        override fun onError(errorCode: Int) {
            super.onError(errorCode)
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            agoraUid = uid

        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
            Toast.makeText(
                this@ConversationLiveRoomActivity,
                String.format("channel leaved"),
                Toast.LENGTH_LONG
            ).show()

        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
        }

    }

    private fun showApiCallErrorToast() {
        Toast.makeText(this, "Something went wrong. Please try Again!!!", Toast.LENGTH_SHORT).show()
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
        val speakerQuery =
            notebookRef.whereEqualTo("is_speaker", true)
        val speakerOptions: FirestoreRecyclerOptions<LiveRoomUser> =
            FirestoreRecyclerOptions.Builder<LiveRoomUser>()
                .setQuery(speakerQuery, LiveRoomUser::class.java)
                .build()
        val listenerQuery = notebookRef.whereEqualTo("is_speaker", false)
        val listenerOptions: FirestoreRecyclerOptions<LiveRoomUser> =
            FirestoreRecyclerOptions.Builder<LiveRoomUser>()
                .setQuery(listenerQuery, LiveRoomUser::class.java)
                .build()
        speakerAdapter?.notifyDataSetChanged()
        listenerAdapter?.notifyDataSetChanged()
        speakerAdapter = SpeakerAdapter(speakerOptions, this, isRoomCreatedByUser)
        listenerAdapter = SpeakerAdapter(listenerOptions, this, isRoomCreatedByUser)
        binding.speakersList.layoutManager = GridLayoutManager(this, 3)
        binding.listenerList.layoutManager = GridLayoutManager(this, 4)
        binding.speakersList.setHasFixedSize(false)
        binding.listenerList.setHasFixedSize(false)
        binding.speakersList.adapter = speakerAdapter
        binding.listenerList.adapter = listenerAdapter

        listenerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
                val userUid = documentSnapshot?.id?.toInt()
                val liveRoomUser = documentSnapshot?.toObject(LiveRoomUser::class.java)
                val roomInfo = ConversationRoomBottomSheetInfo(
                    isRoomCreatedByUser,
                    isRoomUserSpeaker,
                    false,
                    liveRoomUser?.name ?: "",
                    liveRoomUser?.photo_url ?: "",
                    userUid == agoraUid
                )
                showBottomSheet(roomInfo, liveRoomUser?.mentor_id ?: "", userUid)
            }

        })

        speakerAdapter?.setOnItemClickListener(object : SpeakerAdapter.OnUserItemClickListener {
            override fun onItemClick(documentSnapshot: DocumentSnapshot?, position: Int) {
                val userUid = documentSnapshot?.id?.toInt()
                val liveRoomUser = documentSnapshot?.toObject(LiveRoomUser::class.java)
                val roomInfo = ConversationRoomBottomSheetInfo(
                    isRoomCreatedByUser,
                    isRoomUserSpeaker,
                    true,
                    liveRoomUser?.name ?: "",
                    liveRoomUser?.photo_url ?: "",
                    userUid == agoraUid
                )
                showBottomSheet(roomInfo, liveRoomUser?.mentor_id ?: "", userUid)
            }

        })

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
                        UserProfileActivity.startUserProfileActivity(
                            this@ConversationLiveRoomActivity,
                            mentorId,
                            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        )

                    }

                    override fun moveToAudience() {
                        // move to audience
                        val reference = notebookRef.document(userUid.toString())
                        reference.update("is_speaker", false)
                            .addOnSuccessListener {

                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@ConversationLiveRoomActivity,
                                    "fail update ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                    override fun moveToSpeaker() {
                        val reference = notebookRef.document(userUid.toString())
                        reference.update("is_speaker", true)
                            .addOnSuccessListener {

                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@ConversationLiveRoomActivity,
                                    "fail update ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                })
        bottomSheet.show(supportFragmentManager, "Bottom sheet")

    }

    fun openRaisedHandsBottomSheet() {
        val bottomSheet = RaisedHandsBottomSheet.newInstance(roomId ?: 0)
        bottomSheet.show(supportFragmentManager, "Bottom sheet Hands Raised")
    }

    override fun onStart() {
        super.onStart()
        speakerAdapter?.startListening()
        listenerAdapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        speakerAdapter?.stopListening()
        listenerAdapter?.stopListening()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        engine?.leaveChannel()
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
        // accept
    }

    override fun onRejectNotification() {
        binding.notificationBar.visibility = View.GONE
    }
}