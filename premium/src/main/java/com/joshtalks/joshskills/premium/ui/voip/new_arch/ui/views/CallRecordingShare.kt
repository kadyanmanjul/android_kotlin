package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.base.constants.ANALYTICS_EVENT
import com.joshtalks.joshskills.base.constants.SERVICE_ACTION_INCOMING_CALL
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.showToast
import com.joshtalks.joshskills.premium.databinding.ActivityCallRecordingShareBinding
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.track.CONVERSATION_ID
import com.joshtalks.joshskills.premium.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.premium.ui.referral.REFERRAL_SHARE_TEXT_SHARABLE_VIDEO
import com.joshtalks.joshskills.premium.ui.special_practice.utils.WHATSAPP_PACKAGE_STRING
import com.joshtalks.joshskills.premium.ui.video_player.VIDEO_URL
import com.joshtalks.joshskills.premium.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.premium.util.DeepLinkUtil
import com.joshtalks.joshskills.voip.data.CallingRemoteService


class CallRecordingShare : AppCompatActivity() {
    private val binding by lazy<ActivityCallRecordingShareBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_call_recording_share)
    }
    var videoUrl:String = ""
    var conversationId:String = ""
    var currentVideoProgressPosition :Int = 0

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoView.progress = progress
                binding.videoView.onResume()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.executePendingBindings()
        getIntentExtra()
        binding.materialCardView.setOnClickListener {
            addAnalytics("share_btn_click")
            getDeepLinkAndInviteFriends(videoUrl)
        }
       addAnalytics("notification")
        playRecordedVideo()

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    fun addAnalytics(event : String){
        val remoteServiceIntent = Intent(this, CallingRemoteService::class.java)
        remoteServiceIntent.putExtra("event",event)
        remoteServiceIntent.action = ANALYTICS_EVENT
        this.startService(remoteServiceIntent)
    }

    fun playRecordedVideo() {
        binding.videoView.visibility = View.VISIBLE
        binding.videoView.seekToStart()
        binding.videoView.setUrl(videoUrl)
        binding.videoView.onStart()
        playRecordedVideo111()
        binding.videoView.downloadStreamPlay()

        binding.videoView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 15f)
            }
        }
        binding.videoView.clipToOutline = true
        binding.videoView.setFullScreenListener {
            val currentVideoProgressPosition = binding.videoView.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                   this,
                    EMPTY,
                    null,
                    videoUrl,
                    currentVideoProgressPosition,
                    conversationId
                )
            )
        }
    }

    private fun playRecordedVideo111() {
        binding.videoView.setFullScreenListener {
            val currentVideoProgressPosition = binding.videoView.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    this,
                    EMPTY,
                    null,
                    videoUrl,
                    currentVideoProgressPosition,
                    conversationId
                )
            )
        }
    }

    companion object {
        fun getActivityIntentForSharableCallRecording(
            context: Context,
            videoUrl: String?,
            currentVideoProgressPosition: Long = 0,
            conversationId: String? = null,
        ): Intent {
            return Intent(context, CallRecordingShare::class.java).apply {
                putExtra(VIDEO_URL, videoUrl)
                putExtra(CURRENT_VIDEO_PROGRESS_POSITION, currentVideoProgressPosition)
                putExtra(CONVERSATION_ID, conversationId)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        }
    }

    fun getIntentExtra(){
        videoUrl = intent.getStringExtra(VIDEO_URL).toString()
        conversationId = intent.getStringExtra(CONVERSATION_ID).toString()
        currentVideoProgressPosition = intent.getIntExtra(CURRENT_VIDEO_PROGRESS_POSITION,0)
    }

    override fun onPause() {
        binding.videoView.onPause()
        super.onPause()
    }

    fun getDeepLinkAndInviteFriends(videoPath: String) {
        DeepLinkUtil(AppObjectController.joshApplication)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriends(
                        packageString = WHATSAPP_PACKAGE_STRING,
                        dynamicLink = deepLink,
                        videoUrl = videoPath
                    )
                }
            })
            .build()
    }

    fun inviteFriends(packageString: String? = null, dynamicLink: String,videoUrl:String) {
        var referralText =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(REFERRAL_SHARE_TEXT_SHARABLE_VIDEO)
        referralText = referralText.plus("\n").plus(dynamicLink)
        try {
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "*/*"
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(videoUrl)

            )
            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }
}