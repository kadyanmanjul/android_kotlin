package com.joshtalks.joshskills.ui.video_player

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.chat.VIDEO_OPEN_REQUEST_CODE
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME

const val VIDEO_OBJECT = "video_"

class VideoPlayerActivity : BaseActivity(), VideoPlayerEventListener {

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

        const val VIDEO_URL = "video_url"
        const val VIDEO_ID = "video_id"

    }

    private var countUpTimer = CountUpTimer(true)

    init {
        countUpTimer.lap()
    }

    private lateinit var binding: ActivityVideoPlayer1Binding
    private lateinit var chatObject: ChatModel
    private var videoViewGraphList = mutableSetOf<Graph>()
    private var graph: Graph? = null
    private var videoId: String? = null
    private var videoUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppAnalytics.create(AnalyticsEvent.VIDEO_WATCH_ACTIVITY.NAME).push()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_player_1)
        binding.handler = this
        setToolbar()
        binding.videoPlayer.setVideoPlayerEventListener(this)

        if (intent.hasExtra(VIDEO_OBJECT)) {
            chatObject = intent.getParcelableExtra(VIDEO_OBJECT) as ChatModel
            videoId = chatObject.question?.videoList?.getOrNull(0)?.id
            feedbackEngagementStatus(chatObject.question)

            if (chatObject.url != null) {
                if (chatObject.downloadedLocalPath.isNullOrEmpty()) {
                    videoUrl = chatObject.url
                } else {
                    Utils.fileUrl(chatObject.downloadedLocalPath, chatObject.url)?.run {
                        videoUrl = this
                        AppObjectController.videoDownloadTracker.download(
                            chatObject,
                            Uri.parse(chatObject.url),
                            VideoDownloadController.getInstance().buildRenderersFactory(true)
                        )
                    }
                }
            } else {
                videoUrl = chatObject.question?.videoList?.getOrNull(0)?.video_url
            }
        }
        if (intent.hasExtra(VIDEO_URL)) {
            videoUrl = intent.getStringExtra(VIDEO_URL)
        }
        if (intent.hasExtra(VIDEO_ID)) {
            videoId = intent.getStringExtra(VIDEO_ID)
        }

        binding.videoPlayer.setUrl(videoUrl)
        binding.videoPlayer.playVideo()
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
            binding.videoPlayer.openVideoPlayerOptions()
        }
        binding.videoPlayer.getToolbar()?.setNavigationOnClickListener {
            this.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
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
    }


    override fun onBackPressed() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setResult()
        this@VideoPlayerActivity.finish()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playWhenReady) {
            countUpTimer.resume()
        } else {
            countUpTimer.pause()
        }
        if (playbackState == Player.STATE_ENDED) {
            onBackPressed()
        }
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

    fun setResult() {
        // if (videoViewGraphList.isNotEmpty() || graph != null) {
        val resultIntent = Intent()
        resultIntent.putExtra(VIDEO_OBJECT, chatObject)
        setResult(Activity.RESULT_OK, resultIntent)

        //}
    }

}