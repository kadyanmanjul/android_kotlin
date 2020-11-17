package com.joshtalks.joshskills.ui.video_player

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.Download
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.CountUpTimer
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.UsbEventReceiver
import com.joshtalks.joshskills.core.interfaces.UsbEventListener
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.messaging.RxBus2.publish
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.VideoEngage
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper.isVideoPresentInUpdatedChat
import com.joshtalks.joshskills.ui.chat.VIDEO_OPEN_REQUEST_CODE
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


const val VIDEO_OBJECT = "video_"
const val VIDEO_WATCH_TIME = "video_watch_time"
const val IS_BATCH_CHANGED = "is_batch_changed"
const val NEXT_VIDEO_AVAILABLE = "next_video_available"
const val LAST_VIDEO_INTERVAL = "last_video_interval"
const val LAST_LESSON_INTERVAL = "last_lesson_interval"
const val TAG = "video_watch_time"
const val DURATION = "duration"
const val VIDEO_URL = "video_url"
const val VIDEO_ID = "video_id"

class VideoPlayerActivity : BaseActivity(), VideoPlayerEventListener, UsbEventListener {

    companion object {
        fun startConversionActivity(
            activity: Activity,
            chatModel: ChatModel,
            videoTitle: String,
            duration: Int? = 0
        ) {
            val intent = Intent(activity, VideoPlayerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            intent.putExtra(COURSE_NAME, videoTitle)
            intent.putExtra(DURATION, duration)
            activity.startActivityForResult(intent, VIDEO_OPEN_REQUEST_CODE)
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
    private var videoDuration: Long? = 0
    private var courseDuration: Int = 0
    private var nextButtonVisible = false
    private var backPressed = false
    private var searchingNextUrl = false
    private var isBatchChanged = false
    private val handler = Handler()
    private var videoList: List<VideoType> = emptyList()
    private var maxInterval: Int = -1
    private var interval = -1
    private var courseId: Int = -1


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
            chatObject?.run {
                question?.let { question ->
                    courseId = question.course_id
                }
                videoId = this.question?.videoList?.getOrNull(0)?.id
                feedbackEngagementStatus(this.question)
                DatabaseUtils.updateLastUsedModification(this.chatId)
                chatObject?.question?.interval?.let {
                    WorkManagerAdmin.determineNPAEvent(
                        NPSEvent.WATCH_VIDEO,
                        it, chatObject?.question?.questionId
                    )
                }

                if (chatObject?.url != null) {
                    if (chatObject?.downloadedLocalPath.isNullOrEmpty()) {
                        videoUrl = this.url
                    } else {
                        Utils.fileUrl(this.downloadedLocalPath, this.url)?.run {
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

                interval = chatObject!!.question?.interval ?: -1

            }
        }
        if (intent.hasExtra(VIDEO_URL)) {
            videoUrl = intent.getStringExtra(VIDEO_URL)
        }
        if (intent.hasExtra(DURATION)) {
            courseDuration = intent.getIntExtra(DURATION, 0)
        }
        if (intent.hasExtra(VIDEO_ID)) {
            videoId = intent.getStringExtra(VIDEO_ID)
        }

        videoUrl?.run {
            binding.videoPlayer.setUrl(this)
            binding.videoPlayer.playVideo()
            binding.videoPlayer.getCurrentPosition()
        }

        chatObject?.let {
            appAnalytics.addParam(AnalyticsEvent.VIDEO_ID.NAME, videoId)
            appAnalytics.addParam(
                AnalyticsEvent.VIDEO_DURATION.NAME,
                it.mediaDuration?.toString() ?: ""
            )

            binding.progressHorizontal.setOnClickListener {
                playNextVideo()
            }

            binding.close.setOnClickListener {
                this.onBackPressed()
            }
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
        pushAnalyticsEvents(true)
    }


    override fun onBackPressed() {
        backPressed = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setResult()
        this@VideoPlayerActivity.finish()
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int, duration: Long?) {
        duration?.let {
            this.videoDuration = it
        }
        if (playWhenReady) {
            if (usbConnected) {
                binding.videoPlayer.pausePlayer()
                showUsbConnectedMsg()
                countUpTimer.pause()
            } else {
                appAnalytics.addParam(AnalyticsEvent.VIDEO_PLAY.NAME, true)
                countUpTimer.resume()
            }
        } else {
            countUpTimer.pause()
            appAnalytics.addParam(AnalyticsEvent.VIDEO_PAUSE.NAME, true)

        }
        if (playbackState == Player.STATE_ENDED) {
            if (nextButtonVisible.not()) {
                onBackPressed()
            } else {
                binding.videoPlayer.hideButtons()
                binding.imageBlack.visibility = View.VISIBLE
            }
        }
    }

    private fun showUsbConnectedMsg() {
        if (!isFinishing) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.usb_disconnect_title))
                .setMessage(getString(R.string.usb_connected_message))
                .setPositiveButton(
                    getString(R.string.ok_got_it_lbl)
                ) { dialog, _ -> dialog.dismiss() }.show()
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
        publish(
            MediaProgressEventBus(
                Download.STATE_DOWNLOADING, "0", time.toFloat()
            )
        )
        if (searchingNextUrl.not()
            && (videoDuration?.minus(time))!! < 2500
            && chatObject?.conversationId.isNullOrBlank().not()
            && chatObject?.sender?.user?.id.isNullOrBlank().not()
        ) {
            getNextClassUrl()
        }

        graph?.endTime = time
    }


    private fun getNextClassUrl() {
        searchingNextUrl = true
        CoroutineScope(Dispatchers.IO).launch {

            chatObject?.conversationId?.let {
                maxInterval =
                    AppObjectController.appDatabase.chatDao().getMaxIntervalForVideo(it)
            }
            videoList = emptyList()

            val inboxActivity = AppObjectController.appDatabase.courseDao()
                .chooseRegisterCourseMinimal(chatObject?.conversationId!!)
            if (isBatchChanged.not()) {
                checkInDbForNextVideo(inboxActivity)
            }

            if (videoList.isNullOrEmpty().not()) {
                chatObject?.question?.videoList = videoList
                videoList[0].let { videoType ->
                    setVideoObject(videoType)
                    initiatePlaySequence()
                }
            } else {
                try {
                    if (interval < courseDuration) {
                        val response =
                            AppObjectController.chatNetworkService.changeBatchRequest(chatObject?.conversationId!!)
                        val arguments = mutableMapOf<String, String>()
                        val (key, value) = PrefManager.getLastSyncTime(chatObject?.conversationId!!)
                        arguments[key] = value
                        if (response.isSuccessful) {
                            isBatchChanged = true
                            val videoType =
                                isVideoPresentInUpdatedChat(chatObject?.conversationId!!, arguments)
                            videoType?.let {
                                interval = it.interval
                                setVideoObject(videoType)
                                initiatePlaySequence()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            }
        }
    }

    private suspend fun checkInDbForNextVideo(inboxActivity: InboxEntity?) {
        if (interval == courseDuration) {
            return
        }
        while (maxInterval > interval) {
            interval++
            val question: Question? = AppObjectController.appDatabase.chatDao()
                .getQuestionForNextInterval(
                    inboxActivity?.courseId!!, interval
                )

            if (question != null) {
                chatObject!!.question = question
                if (question.material_type == BASE_MESSAGE_TYPE.VI && question.type == BASE_MESSAGE_TYPE.Q) {
                    val videoType = AppObjectController.appDatabase.chatDao()
                        .getVideosOfQuestion(questionId = question.questionId)
                    videoList = videoType
                    break
                }
            }
        }
    }

    fun setVideoObject(videoType: VideoType) {
        videoType.video_url?.run {
            videoId = videoType.id
            videoUrl = videoType.video_url
        }
    }

    private fun initiatePlaySequence() {
        logNextVideoTimerEvent()
        CoroutineScope(Dispatchers.Main).launch {
            binding.frameProgress.visibility = View.VISIBLE
            binding.toolbar.visibility = View.GONE
            binding.videoPlayer.hideButtons()
            nextButtonVisible = true
            startProgress()
        }
    }

    private fun logNextVideoTimerEvent() {
        AppAnalytics.create(AnalyticsEvent.NEXT_VIDEO_TIMER_STARTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun logNextVideoClickedEvent() {
        AppAnalytics.create(AnalyticsEvent.NEXT_VIDEO_BUTTON_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

    private fun startProgress() {
        Thread {
            binding.progressHorizontal.progress = 0
            while (binding.progressHorizontal.progress < 100) {
                handler.post {
                    binding.progressHorizontal.progress += 1
                }
                try {
                    // Sleep for 100 milliseconds.
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            this@VideoPlayerActivity.runOnUiThread {
                if (nextButtonVisible && backPressed.not()) {
                    playNextVideo()
                }
            }
        }.start()
    }

    private fun playNextVideo() {

        videoUrl?.run {
            binding.frameProgress.visibility = View.GONE
            binding.toolbar.visibility = View.VISIBLE
            binding.imageBlack.visibility = View.GONE
            binding.videoPlayer.playNextVideo(videoUrl)
            nextButtonVisible = false
            searchingNextUrl = false
            pushPreviousAnalyticsEvents()
            showToast(getString(R.string.next_class_started))
        }
    }

    private fun pushPreviousAnalyticsEvents() {
        logNextVideoClickedEvent()
        pushAnalyticsEvents(false)
        countUpTimer.reset()
        appAnalytics.addParam(AnalyticsEvent.VIDEO_ID.NAME, videoId)
        videoViewGraphList.clear()

        if (graph != null) {
            graph = null
        }
        graph = Graph(binding.videoPlayer.player!!.currentPosition)

    }

    private fun pushAnalyticsEvents(flowFromOnStop: Boolean) {
        try {
            appAnalytics.push()
            onPlayerReleased()
            if (flowFromOnStop) {
                graph?.endTime = binding.videoPlayer.player!!.currentPosition
                graph?.let {
                    videoViewGraphList.add(it)
                }
                graph = null
            }

            videoId?.run {
                EngagementNetworkHelper.engageVideoApi(
                    VideoEngage(
                        videoViewGraphList.toList(),
                        this.toInt(),
                        countUpTimer.time.toLong(),
                        courseID = courseId,
                    )
                )
            }
        } catch (ex: Exception) {
        }
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
            resultIntent.putExtra(IS_BATCH_CHANGED, isBatchChanged)
            resultIntent.putExtra(LAST_VIDEO_INTERVAL, interval)
            resultIntent.putExtra(NEXT_VIDEO_AVAILABLE, nextButtonVisible)
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
        if (BuildConfig.DEBUG) {
            return
        }
        setBroadcastReceivers()
    }

    override fun onUsbConnect() {
        if (BuildConfig.DEBUG) {
            return
        }
        showUsbConnectedMsg()
        binding.videoPlayer.pausePlayer()
        usbConnected = true
    }

    override fun onUsbDisconnect() {
        usbConnected = false
    }

}
