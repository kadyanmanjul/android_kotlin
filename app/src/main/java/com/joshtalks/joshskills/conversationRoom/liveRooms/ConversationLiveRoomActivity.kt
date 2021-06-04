package com.joshtalks.joshskills.conversationRoom.liveRooms

import android.content.DialogInterface
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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.interfaces.ConversationLiveRoomSpeakerClickAction
import com.joshtalks.joshskills.databinding.ActivityConversationLiveRoomBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ChannelMediaOptions
import kotlin.math.abs

class ConversationLiveRoomActivity : BaseActivity(), ConversationLiveRoomSpeakerClickAction {
    lateinit var binding: ActivityConversationLiveRoomBinding
    lateinit var viewModel: ConversationLiveRoomViewModel
    val firebaseFirestore = FirebaseFirestore.getInstance()
    var roomId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    var speakerAdapter: SpeakerAdapter? = null
    var listenerAdapter: SpeakerAdapter? = null
    lateinit var notebookRef: CollectionReference
    private var engine: RtcEngine? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var moderatorUid: Int? = null
    var iSSoundOn = true
    var isHandRaised = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationLiveRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ConversationLiveRoomViewModel()
        channelName = intent?.getStringExtra("CHANNEL_NAME")
        agoraUid = intent?.getIntExtra("UID", 0)
        token = intent?.getStringExtra("TOKEN")
        roomId = intent?.getIntExtra("ROOM_ID", 0)
        isRoomCreatedByUser = intent.getBooleanExtra("IS_ROOM_CREATED_BY_USER", false)
        if (isRoomCreatedByUser) {
            binding.handRaiseBtn.visibility = View.GONE
        } else {
            binding.handRaiseBtn.visibility = View.VISIBLE
        }

        notebookRef = firebaseFirestore.collection("conversation_rooms").document(roomId.toString())
            .collection("users")
        setUpRecyclerView()
        setLeaveEndButton(isRoomCreatedByUser)
        engine = AppObjectController.getRtcEngine(AppObjectController.joshApplication)
        leaveRoomIfModeratorEndRoom()
        // Check permission
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

        switchingRoles()

        binding.muteBtn.setOnClickListener {
            if (iSSoundOn) {
                val reference = notebookRef.document(agoraUid.toString())
                reference.update("is_mic_on", false)
                    .addOnSuccessListener {
                        iSSoundOn = false
                        engine?.enableLocalAudio(iSSoundOn)
                        binding.muteBtn.text = "UnMute"
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "fail update ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            } else {
                val reference = notebookRef.document(agoraUid.toString())

                reference.update("is_mic_on", true)
                    .addOnSuccessListener {
                        iSSoundOn = true
                        engine?.enableLocalAudio(iSSoundOn)
                        binding.muteBtn.text = "Mute"
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "fail update ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        binding.handRaiseBtn.setOnClickListener {
            if (isHandRaised) {
                val reference = notebookRef.document(agoraUid.toString())
                reference.update("is_hand_raised", true)
                    .addOnSuccessListener {
                        isHandRaised = !isHandRaised
                        binding.handRaiseBtn.text = "Raised"
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "fail update ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

            } else {
                val reference = notebookRef.document(agoraUid.toString())

                reference.update("is_hand_raised", false)
                    .addOnSuccessListener {
                        isHandRaised = !isHandRaised
                        binding.handRaiseBtn.text = "UnRaised"
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "fail update ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

    }

    private fun switchingRoles() {
        notebookRef.document(agoraUid.toString()).addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (value != null) {
                Log.d(TAG, "${value["is_speaker"]}")
                val isUserSpeaker = value["is_speaker"]
                val isMicOn = value["is_mic_on"]
                val isHandRaisedValue = value["is_hand_raised"]
                if (!isRoomCreatedByUser) {
                    if (isUserSpeaker == true) {
                        engine!!.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
                        Log.d(TAG, "onUserJoined: user $agoraUid client role set to broadcaster")
                        binding.muteBtn.visibility = View.VISIBLE
                        binding.handRaiseBtn.visibility = View.GONE
                        iSSoundOn = isMicOn == true
                        engine?.enableLocalAudio(iSSoundOn)
                        if (iSSoundOn) {
                            binding.muteBtn.text = "Mute"
                        } else {
                            binding.muteBtn.text = "UnMute"
                        }
                    } else {
                        engine!!.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
                        Log.d(TAG, "onUserJoined: user $agoraUid client role set to audience")
                        binding.muteBtn.visibility = View.GONE
                        binding.handRaiseBtn.visibility = View.VISIBLE
                        isHandRaised = isHandRaisedValue == true
                        if (isHandRaised) {
                            binding.handRaiseBtn.text = "Raised"
                        } else {
                            binding.handRaiseBtn.text = "UnRaised"
                        }
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
            Log.d(TAG, "takePermissions: All okay already")
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            joinChannel(channelName)
                            Log.d(TAG, "onPermissionsChecked: all allowed")
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@ConversationLiveRoomActivity,
                                "Permission Denied ",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "onPermissionsChecked: Some permission denied")
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
                    Log.d(TAG, "onPermissionRationaleShouldBeShown: ")
                }
            }
        )

    }

    private fun joinChannel(channelName: String?) {
        engine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        if (isRoomCreatedByUser) {
            engine!!.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER)
            Log.d(TAG, "joinChannel: client role broadcaster")
        } else {
            engine!!.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_AUDIENCE)
            Log.d(TAG, "joinChannel: client role audience")
        }
        if (eventListener != null) {
            engine?.removeHandler(eventListener)
        }
        if (eventListener != null) {
            engine?.addHandler(eventListener)
        }

        engine!!.enableAudioVolumeIndication(1000, 3, true)

        val option = ChannelMediaOptions()
        option.autoSubscribeAudio = true
        option.autoSubscribeVideo = true
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
            Log.d(TAG, "onError: $errorCode")
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            agoraUid = uid
            Log.d(TAG, "onJoinChannelSuccess: $uid")
            Toast.makeText(
                this@ConversationLiveRoomActivity,
                "Channel joined successfully",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onLeaveChannel(stats: RtcStats) {
            super.onLeaveChannel(stats)
            Toast.makeText(
                this@ConversationLiveRoomActivity,
                String.format("channel leaved"),
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "onLeaveChannel: $stats")

        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            Log.d(TAG, "onUserJoined: user $uid")
            Toast.makeText(
                this@ConversationLiveRoomActivity,
                String.format("user %d joined!", uid),
                Toast.LENGTH_LONG
            ).show()
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            Log.d(TAG, "onUserOffline: user $uid  $reason")
        }

        override fun onRejoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            super.onRejoinChannelSuccess(channel, uid, elapsed)
            Log.d(TAG, "onRejoinChannelSuccess: user $uid ")
        }

        override fun onConnectionLost() {
            super.onConnectionLost()
            Log.d(TAG, "onConnectionLost: ")
        }

    }


    private fun showApiCallErrorToast() {
        Toast.makeText(this, "Something went wrong. Please try Again!!!", Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(message: String?) {
        AlertDialog.Builder(this).setTitle("Tips").setMessage(message)
            .setPositiveButton("OK") { dialog: DialogInterface, which: Int -> dialog.dismiss() }
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
        super.onDestroy()
        if (engine != null) {
            engine?.leaveChannel()
            engine = null
        }
    }

    companion object {
        private const val TAG = "ConversationLiveRoomAct"
    }
}