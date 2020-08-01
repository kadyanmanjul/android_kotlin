package com.joshtalks.joshskills.ui.video_player

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CountUpTimer
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.UsbEventReceiver
import com.joshtalks.joshskills.core.interfaces.UsbEventListener
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.VIDEO_OPEN_REQUEST_CODE
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME

const val VIDEO_OBJECT = "video_"
const val VIDEO_WATCH_TIME = "video_watch_time"

class VideoPlayerActivity : BaseActivity(), VideoPlayerEventListener, UsbEventListener {

    companion object {
        fun startConversionActivity(
            activity: Activity,
            chatModel: ChatModel,
            videoTitle: String
        ) {
            val intent = Intent(activity, VideoPlayerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            intent.putExtra(COURSE_NAME, videoTitle)
            activity.startActivityForResult(intent, VIDEO_OPEN_REQUEST_CODE)
        }

        fun startConversionActivityV2(
            context: Context,
            videoTitle: String?,
            videoId: String?,
            videoUrl: String?

        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(VIDEO_URL, videoUrl)
            intent.putExtra(VIDEO_ID, videoId)
            intent.putExtra(COURSE_NAME, videoTitle)
            context.startActivity(intent)
        }

        fun startVideoActivity(
            context: Context,
            videoTitle: String?,
            videoId: String?,
            videoUrl: String?

        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(VIDEO_URL, videoUrl)
            intent.putExtra(VIDEO_ID, videoId)
            intent.putExtra(COURSE_NAME, videoTitle)
            context.startActivity(intent)
        }

        const val VIDEO_URL = "video_url"
        const val VIDEO_ID = "video_id"

    }

    private lateinit var usbEventReceiver: UsbEventReceiver
    private var usbConnected: Boolean = false
    private var countUpTimer = CountUpTimer(true)

    init {
        countUpTimer.lap()
    }

    private lateinit var binding: ActivityVideoPlayer1Binding
    private var chatObject: ChatModel? = null
    private var videoViewGraphList = mutableSetOf<Graph>()
    private var graph: Graph? = null
    private var videoId: String? = null
    private var videoUrl: String? = null
    private lateinit var appAnalytics: AppAnalytics


    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.VIDEO_WATCH_ACTIVITY.NAME)
            .addBasicParam()
            .addUserDetails()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_player_1)
        binding.handler = this
        setToolbar()
        binding.videoPlayer.setVideoPlayerEventListener(this)

        if (intent.hasExtra(VIDEO_OBJECT)) {
            chatObject = intent.getParcelableExtra(VIDEO_OBJECT) as ChatModel
            videoId = chatObject?.question?.videoList?.getOrNull(0)?.id
            feedbackEngagementStatus(chatObject?.question)
            chatObject?.question?.interval?.let {
                WorkMangerAdmin.determineNPAEvent(
                    NPSEvent.WATCH_VIDEO,
                    it, chatObject?.question?.questionId
                )
            }

            if (chatObject?.url != null) {
                if (chatObject?.downloadedLocalPath.isNullOrEmpty()) {
                    videoUrl = chatObject?.url
                } else {
                    Utils.fileUrl(chatObject?.downloadedLocalPath, chatObject?.url)?.run {
                        videoUrl = this
                        AppObjectController.videoDownloadTracker.download(
                            chatObject,
                            Uri.parse(chatObject?.url),
                            VideoDownloadController.getInstance().buildRenderersFactory(true)
                        )
                    }
                }
            } else {
                videoUrl = chatObject?.question?.videoList?.getOrNull(0)?.video_url
            }
        }
        if (intent.hasExtra(VIDEO_URL)) {
            videoUrl = intent.getStringExtra(VIDEO_URL)
        }
        if (intent.hasExtra(VIDEO_ID)) {
            videoId = intent.getStringExtra(VIDEO_ID)
        }

        videoUrl?.run {
            binding.videoPlayer.setUrl(this)
            binding.videoPlayer.playVideo()
        }


        chatObject?.let {
            appAnalytics.addParam(AnalyticsEvent.VIDEO_ID.NAME, it.chatId)
            appAnalytics.addParam(
                AnalyticsEvent.VIDEO_DURATION.NAME,
                it.mediaDuration?.toString() ?: ""
            )

        }

        appAnalytics.addParam(
            AnalyticsEvent.COURSE_NAME.NAME,
            binding.textMessageTitle.text.toString()
        )

    }


    private fun setToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.videoPlayer.setToolbar(binding.toolbar)
        intent.getStringExtra(COURSE_NAME)?.let {
            binding.textMessageTitle.text = it
        }
        binding.ivBack.setOnClickListener {
            this.onBackPressed()
        }
        binding.ivMore.setOnClickListener {
            appAnalytics.addParam(AnalyticsEvent.VIDEO_MORE_ACTIONS.NAME, true)
            binding.videoPlayer.openVideoPlayerOptions()
        }
        binding.videoPlayer.getToolbar()?.setNavigationOnClickListener {
            this.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            appAnalytics.push()
            onPlayerReleased()
            videoId?.run {
                EngagementNetworkHelper.engageVideoApi(
                    VideoEngage(
                        videoViewGraphList.toList(),
                        this.toInt(),
                        countUpTimer.time.toLong()
                    )
                )
            }
        } catch (ex: Exception) {
        }
        unregisterReceiver(usbEventReceiver)
    }


    override fun onBackPressed() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setResult()
        this@VideoPlayerActivity.finish()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady) {
            if (usbConnected) {
                binding.videoPlayer.pausePlayer()
                showUsbConnectedMsg()
            } else {
                appAnalytics.addParam(AnalyticsEvent.VIDEO_PLAY.NAME, true)
                countUpTimer.resume()
            }
        } else {
            countUpTimer.pause()
            appAnalytics.addParam(AnalyticsEvent.VIDEO_PAUSE.NAME, true)

        }
        if (playbackState == Player.STATE_ENDED) {
            onBackPressed()
        }
    }

    private fun showUsbConnectedMsg() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.usb_disconnect_title))
            .setMessage(getString(R.string.usb_connected_message))
            .setPositiveButton(
                getString(R.string.ok_got_it_lbl)
            ) { dialog, _ -> dialog.dismiss() }.show()
    }

    override fun onPlayerReady() {
        if (graph != null) {
            return
        }
        graph = Graph(binding.videoPlayer.player!!.currentPosition)
    }

    override fun helpAndFeedback() {
        openHelpActivity()
    }


    override fun onClickFullScreenView(cOrientation: Int) {

    }


    override fun onCurrentTimeUpdated(time: Long) {
        graph?.endTime = time
    }

    override fun onPlayerReleased() {
        graph?.endTime = binding.videoPlayer.player!!.currentPosition
        graph?.let {
            videoViewGraphList.add(it)
        }
        graph = null
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
        graph?.endTime = lastPos
        graph?.let {
            videoViewGraphList.add(it)
        }
        graph = null
    }

    private fun setResult() {
        val resultIntent = Intent()
        chatObject?.run {
            resultIntent.putExtra(VIDEO_OBJECT, this)
            resultIntent.putExtra(VIDEO_WATCH_TIME, countUpTimer.time)
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }


    private fun setBroadcastReceivers() {
        usbEventReceiver = UsbEventReceiver()
        usbEventReceiver.listener = this
        val filter = IntentFilter()
        filter.addAction("android.hardware.usb.action.USB_STATE")
        registerReceiver(usbEventReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        setBroadcastReceivers()
    }

    override fun onUsbConnect() {
        showUsbConnectedMsg()
        binding.videoPlayer.pausePlayer()
        usbConnected = true
    }

    override fun onUsbDisconnect() {
        usbConnected = false
    }
}