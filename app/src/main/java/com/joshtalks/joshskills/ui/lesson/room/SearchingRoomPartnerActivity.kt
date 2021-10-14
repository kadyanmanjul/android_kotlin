package com.joshtalks.joshskills.ui.lesson.room

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.*
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.conversationRoom.liveRooms.ConversationLiveRoomActivity
import com.joshtalks.joshskills.conversationRoom.roomsListing.ConversationRoomListingViewModel
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.HAS_SEEN_CONVO_ROOM_POINTS
import com.joshtalks.joshskills.core.IS_CONVERSATION_ROOM_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivitySearchingRoomUserBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID

const val ROOM_ID = "room_id"

class SearchingRoomPartnerActivity : BaseActivity() {

    companion object {
        const val CHANNEL_NAME = "channel_name"
        const val UID = "uid"
        const val TOKEN = "TOKEN"
        const val IS_ROOM_CREATED_BY_USER = "is_room_created_by_user"
        const val ROOM_ID = "room_id"
        const val OPEN_FROM_NOTIFICATION = "open_from_notification"
        const val ROOM_QUESTION_ID = "room_question_id"

        fun startUserForPractiseOnPhoneActivity(
            activity: Activity,
            channelName: String?,
            uid: Int?,
            token: String?,
            isRoomCreatedByUser: Boolean,
            roomId: Int?,
            roomQuestionId: Int? = null,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, SearchingRoomPartnerActivity::class.java).apply {
                putExtra(CHANNEL_NAME, channelName)
                putExtra(UID, uid)
                putExtra(TOKEN, token)
                putExtra(IS_ROOM_CREATED_BY_USER, isRoomCreatedByUser)
                putExtra(ROOM_ID, roomId)
                putExtra(ROOM_QUESTION_ID, roomQuestionId)

                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }

    private lateinit var binding: ActivitySearchingRoomUserBinding
    private var timer: CountDownTimer? = null
    var channelName: String? = null
    var agoraUid: Int? = null
    var token: String? = null
    var roomId: Int? = null
    var roomQuestionId: Int? = null
    var isRoomCreatedByUser: Boolean = false
    var timeCreated: Int = 0
    private val viewModel: ConversationRoomListingViewModel by lazy {
        ViewModelProvider(this).get(ConversationRoomListingViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, true)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_searching_room_user)
        binding.lifecycleOwner = this
        binding.handler = this
        getIntentExtras()
        if (roomId == null || roomId == 0) {
            finish()
        }
        initView()
        addObserver()
        startProgressBarCountDown()
    }

    private fun getIntentExtras() {
        channelName = intent?.getStringExtra(ConversationLiveRoomActivity.CHANNEL_NAME)
        agoraUid = intent?.getIntExtra(ConversationLiveRoomActivity.UID, 0)
        token = intent?.getStringExtra(ConversationLiveRoomActivity.TOKEN)
        roomId = intent?.getIntExtra(ConversationLiveRoomActivity.ROOM_ID, 0)
        roomQuestionId = intent?.getIntExtra(ConversationLiveRoomActivity.ROOM_QUESTION_ID, -1)
        isRoomCreatedByUser =
            intent.getBooleanExtra(ConversationLiveRoomActivity.IS_ROOM_CREATED_BY_USER, false)
    }

    private fun addObserver() {
        Handler(Looper.getMainLooper()).postDelayed({
            FirebaseFirestore.getInstance().collection("conversation_rooms")
                .document(roomId.toString()).collection("users")
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    val list = value?.documents
                    list?.size?.let {
                        if (it >= 2) {
                            timer?.cancel()
                            ConversationLiveRoomActivity.startConversationLiveRoomActivity(
                                this,
                                channelName,
                                agoraUid,
                                token,
                                isRoomCreatedByUser,
                                roomId,
                                roomQuestionId
                            )
                            finish()
                            return@addSnapshotListener
                        }
                    }
                }
        }, 200)

        viewModel.isRoomEnded.observe(this, Observer {
            if (it == true ){
                finish()
            }
        })

    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initView() {
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
    }

    private fun startProgressBarCountDown() {
        runOnUiThread {
            binding.progressBar.max = 100
            binding.progressBar.progress = 0
            timer = object : CountDownTimer(5000, 500) {
                override fun onTick(millisUntilFinished: Long) {
                    timeCreated= timeCreated + 1
                    if (timeCreated>=(60*2*2)){
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
        timer?.cancel()
        PrefManager.put(IS_CONVERSATION_ROOM_ACTIVE, false)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_POINTS,true)
        viewModel.endRoom(roomId.toString(), roomQuestionId)
    }

    override fun onBackPressed() {
        endRoom()
        super.onBackPressed()
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

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

}
