package com.joshtalks.joshskills.ui.video_player

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.Download
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.ktx.googleAnalyticsParameters
import com.google.firebase.dynamiclinks.ktx.shortLinkAsync
import com.google.firebase.ktx.Firebase
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.UsbEventReceiver
import com.joshtalks.joshskills.core.interfaces.UsbEventListener
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import com.joshtalks.joshskills.databinding.ActivityVideoPlayer1Binding
import com.joshtalks.joshskills.messaging.RxBus2.publish
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper.isVideoPresentInUpdatedChat
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.VIDEO_OPEN_REQUEST_CODE
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.referral.*
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
const val IS_SHARABLE_VIDEO = "is_sharable_video"

class VideoPlayerActivity : BaseActivity(), VideoPlayerEventListener, UsbEventListener {

    companion object {
        fun startConversionActivity(
            activity: Activity,
            chatModel: ChatModel,
            videoTitle: String,
            duration: Int? = 0,
            conversationId: String? = null,
            isSharableVideo: Boolean? = null
        ) {
            val intent = Intent(activity, VideoPlayerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.putExtra(VIDEO_OBJECT, chatModel)
            intent.putExtra(COURSE_NAME, videoTitle)
            intent.putExtra(DURATION, duration)
            intent.putExtra(CONVERSATION_ID, conversationId)
            isSharableVideo?.let {
                intent.putExtra(IS_SHARABLE_VIDEO, isSharableVideo)
            }
            activity.startActivityForResult(intent, VIDEO_OPEN_REQUEST_CODE)
        }

        fun startVideoActivity(
            context: Context,
            videoTitle: String?,
            videoId: String?,
            videoUrl: String?,
            currentVideoProgressPosition: Long = 0,
            conversationId: String? = null,
            isSharableVideo: Boolean? = null

        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(VIDEO_URL, videoUrl)
            intent.putExtra(VIDEO_ID, videoId)
            intent.putExtra(COURSE_NAME, videoTitle)
            isSharableVideo?.let {
                intent.putExtra(IS_SHARABLE_VIDEO, isSharableVideo)
            }
            intent.putExtra(CURRENT_VIDEO_PROGRESS_POSITION, currentVideoProgressPosition)
            intent.putExtra(CONVERSATION_ID, conversationId)
            context.startActivity(intent)
        }

        fun getActivityIntent(
            context: Context,
            videoTitle: String?,
            videoId: String?,
            videoUrl: String?,
            currentVideoProgressPosition: Long = 0,
            conversationId: String? = null,
            isSharableVideo: Boolean? = null
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(VIDEO_URL, videoUrl)
                putExtra(VIDEO_ID, videoId)
                putExtra(COURSE_NAME, videoTitle)
                isSharableVideo?.let {
                    putExtra(IS_SHARABLE_VIDEO, isSharableVideo)
                }
                putExtra(CURRENT_VIDEO_PROGRESS_POSITION, currentVideoProgressPosition)
                putExtra(CONVERSATION_ID, conversationId)
            }
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
    private var currentVideoProgressPosition: Long = 0
    private var isSharableVideo: Boolean = false
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
    private var userReferralURL: String = EMPTY

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
            chatObject = intent.getParcelableExtra(VIDEO_OBJECT) as ChatModel?
            chatObject?.run {
                question?.let { question ->
                    courseId = question.course_id
                }
                videoId = this.question?.videoList?.getOrNull(0)?.id
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
        if (intent.hasExtra(CURRENT_VIDEO_PROGRESS_POSITION)) {
            currentVideoProgressPosition = intent.getLongExtra(CURRENT_VIDEO_PROGRESS_POSITION, 0)
        }
        if (intent.hasExtra(IS_SHARABLE_VIDEO)) {
            isSharableVideo = intent.getBooleanExtra(IS_SHARABLE_VIDEO, false)
            if (isSharableVideo) {
                getRefferalCodes()
            }
        }

        videoUrl?.run {
            binding.videoPlayer.setUrl(this)
            binding.videoPlayer.playVideo()
            binding.videoPlayer.getCurrentPosition()
            if (currentVideoProgressPosition > 0) {
                binding.videoPlayer.seekTo(currentVideoProgressPosition)
            }
        }

        chatObject?.let {
            appAnalytics.addParam(AnalyticsEvent.VIDEO_ID.NAME, videoId)
            appAnalytics.addParam(
                AnalyticsEvent.VIDEO_DURATION.NAME,
                it.duration.toString()
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

    private fun getRefferalCodes() {
        if (PrefManager.hasKey(USER_SHARE_SHORT_URL).not()) {
            userReferralURL = PrefManager.getStringValue(USER_SHARE_SHORT_URL)

        }
        Firebase.dynamicLinks.shortLinkAsync(ShortDynamicLink.Suffix.SHORT) {
            link = Uri.parse("https://joshskill.app.link")
            domainUriPrefix = AppObjectController.getFirebaseRemoteConfig().getString(SHARE_DOMAIN)

            androidParameters(BuildConfig.APPLICATION_ID) {
                minimumVersion = 69
            }
            googleAnalyticsParameters {
                source = Mentor.getInstance().referralCode
                medium = "Mobile"
                campaign = "user_referer"
            }

        }.addOnSuccessListener { result ->
            result?.shortLink?.let {
                try {
                    if (it.toString().isNotEmpty()) {
                        PrefManager.put(USER_SHARE_SHORT_URL, it.toString())
                        userReferralURL = it.toString()

                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
            .addOnFailureListener {
                it.printStackTrace()

            }
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
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
            if (isSharableVideo) {
                binding.saveVideoFrame.visibility = View.VISIBLE
                binding.toolbar.visibility = View.GONE
                binding.videoPlayer.hideButtons()
                //setClickListeners()
            } else {
                if (nextButtonVisible.not()) {
                    onBackPressed()
                } else {
                    binding.videoPlayer.hideButtons()
                    binding.imageBlack.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setClickListeners() {
        binding.share.setOnClickListener {
            inviteFriends()
        }

        binding.saveGallery.setOnClickListener {
            videoUrl?.let {
                showToast("Downloading ...")
                downloadVideo(videoUrl!!)
            }
        }
    }

    private fun downloadVideo(videoUrl: String) {
        downloadFile(videoUrl,saveToGallery = true)
    }

    fun inviteFriends(packageString: String? = null) {
        var referralText = com.joshtalks.joshskills.ui.referral.VIDEO_URL.plus("\n").plus(
            AppObjectController.getFirebaseRemoteConfig().getString(REFERRAL_SHARE_TEXT_KEY)
        )
        val refAmount =
            AppObjectController.getFirebaseRemoteConfig().getLong(REFERRAL_EARN_AMOUNT_KEY)
                .toString()
        referralText = referralText.replace(REPLACE_HOLDER, Mentor.getInstance().referralCode)
        referralText = referralText.replace(REFERRAL_AMOUNT_HOLDER, refAmount)

        referralText = if (userReferralURL.isEmpty()) {
            referralText.plus("\n").plus(getAppShareUrl())
        } else {
            referralText.plus("\n").plus(userReferralURL)
        }

        try {
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "text/plain"
            if (packageString.isNullOrEmpty().not()) {
                waIntent.setPackage(packageString)
            }
            waIntent.putExtra(Intent.EXTRA_TEXT, referralText)
            startActivity(Intent.createChooser(waIntent, "Share with"))

        } catch (e: PackageManager.NameNotFoundException) {
            showToast(getString(R.string.whatsApp_not_installed))
        }
    }

    private fun getAppShareUrl(): String {
        return "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "&referrer=utm_source%3D${Mentor.getInstance().referralCode}"
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
        if (searchingNextUrl.not() &&
            videoDuration?.compareTo(0L)!! > 0 &&
            (videoDuration?.minus(time))!! < 2500 &&
            chatObject?.conversationId.isNullOrBlank().not() &&
            chatObject?.sender?.user?.id.isNullOrBlank().not() &&
            isSharableVideo.not()
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
                        val (key, value) = LastSyncPrefManager.getLastSyncTime(chatObject?.conversationId!!)
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
        resultIntent.putExtra(
            CURRENT_VIDEO_PROGRESS_POSITION,
            binding.videoPlayer.getCurrentPosition()
        )
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
