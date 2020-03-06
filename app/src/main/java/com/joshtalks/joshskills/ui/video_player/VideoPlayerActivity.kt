package com.joshtalks.joshskills.ui.video_player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.PlayerListener
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.model.ListenGraph
import com.joshtalks.joshskills.repository.server.engage.VideoEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME

const val VIDEO_OBJECT = "video_"

class VideoPlayerActivity : BaseActivity(), PlayerListener {


    override fun onPlayerReady() {
        if (graph != null) {
            graph?.endTime = binding.pvPlayer.player!!.currentPosition
            videoViewGraphList.add(graph!!)
        }
        graph = null

        graph = ListenGraph(binding.pvPlayer.player!!.currentPosition)

    }

    override fun onBufferingUpdated(isBuffering: Boolean) {


    }

    override fun onCurrentTimeUpdated(time: Long) {
        lastPos = time
    }

    override fun onPlayerReleased() {
        graph?.endTime = binding.pvPlayer.player!!.currentPosition
        graph?.let {
            videoViewGraphList.add(it)
        }
        graph = null
    }

    override fun onPositionDiscontinuity(reason: Int, lastPos: Long) {
        graph?.endTime = lastPos
        graph?.let {
            videoViewGraphList.add(it)
        }
        graph = null
    }

    private lateinit var binding: ActivityVideoPlayer1Binding
    private lateinit var chatObject: ChatModel
    private lateinit var exoProgress: DefaultTimeBar
    var videoViewGraphList = mutableSetOf<ListenGraph>()
    var graph: ListenGraph? = null
    var lastPos: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_video_player_1)
        binding.handler = this
        setToolbar()

        chatObject = intent.getSerializableExtra(VIDEO_OBJECT) as ChatModel

        if (chatObject.url != null) {
            if (chatObject.downloadedLocalPath.isNullOrEmpty()) {
                binding.pvPlayer.setUrl(chatObject.url)
                binding.pvPlayer.downloadStreamPlay()
            } else {
                Utils.fileUrl(chatObject.downloadedLocalPath, chatObject.url)?.run {
                    binding.pvPlayer.setUrl(this)
                    binding.pvPlayer.play()
                    AppObjectController.videoDownloadTracker.download(
                        chatObject,
                        Uri.parse(chatObject.url),
                        VideoDownloadController.getInstance().buildRenderersFactory(false)
                    )
                }

            }
        } else {
            binding.pvPlayer.setUrl(chatObject.question?.videoList?.get(0)?.video_url)
            binding.pvPlayer.downloadStreamPlay()
        }

        binding.pvPlayer.setActivity(this)
        exoProgress = findViewById(R.id.exo_progress)
        AppAnalytics.create(AnalyticsEvent.WATCH_ACTIVITY.NAME).push()
        try {
            binding.pvPlayer.setPlayerControlViewVisibilityListener { visibility ->
                binding.toolbar.visibility = visibility
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setToolbar() {

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.overlay)

        intent.getStringExtra(COURSE_NAME)?.let {
            binding.textMessageTitle.text = it
        }
        binding.ivBack.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            finish()
        }
    }


    companion object {
        fun startConversionActivity(
            context: Context,
            chatModel: ChatModel,
            courseName: String
        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            intent.putExtra(COURSE_NAME, courseName)
            context.startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            binding.pvPlayer.onStart()
        } catch (ex: Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        try {
            chatObject.question?.videoList?.get(0)?.id?.let {
                EngagementNetworkHelper.engageVideoApi(
                    VideoEngage(
                        videoViewGraphList.toList(),
                        it,
                        binding.pvPlayer.lastPosition
                    )
                )
            }
            binding.pvPlayer.onStop()

        } catch (ex: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.pvPlayer.onPause()
        } catch (ex: Exception) {

        }

    }

    override fun onResume() {
        super.onResume()
        try {
            binding.pvPlayer.onResume()
        } catch (ex: Exception) {

        }
    }

    override fun onBackPressed() {
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
        this@VideoPlayerActivity.finish()
    }


}