package com.joshtalks.joshskills.core.videoplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Pair
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_VIDEO_BUFFER_SIZE
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.ui.*
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.*
import java.util.concurrent.TimeUnit

class BitVideoPlayer : PlayerView, LifecycleObserver, PlayerControlView.VisibilityListener,
    TextureView.SurfaceTextureListener {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var uri: Uri? = null
    private var lastPosition: Long = 0
    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val timeHandler = Handler()
    private val controllerHandler = Handler()

    private var mGestureType = GestureType.SwipeGesture
    private var am: AudioManager? = null
    private var mInitialTextureWidth: Int = 0
    private var mInitialTextureHeight: Int = 0
    private var mWindow: Window? = null
    private var mControlsDisabled = false
    private var mDoubleTapSeekDuration: Int = 10000
    private var currentPosition: Long = 0
    private var mWasPlaying: Boolean = false
    private var mSurface: Surface? = null
    private var mSurfaceAvailable: Boolean = false
    private var currentMappedTrackInfoPosition: Int = 0
    private var activity: Activity? = null
    private var lisOfVideoQualityTrack = mutableListOf<VideoQualityTrack>()
    private var videoQualityP: String = ""
    private var playbackSpeed: Float = 1F
    private var playbackSpeedTitle: String = "Normal"
    private var audioLanguage: String = ""
    private var lisOfAudioLanguageTrack = mutableListOf<AudioLanguageTrack>()
    private var playerListener: VideoPlayerEventListener? = null

    private var mToolbar: Toolbar? = null
    private lateinit var defaultTimeBar: DefaultTimeBar
    private lateinit var progressBarBottom: ProgressBar
    private lateinit var imgFullScreenEnterExit: AppCompatImageView
    private lateinit var mPositionTextView: AppCompatTextView
    private lateinit var viewForward: AppCompatTextView
    private lateinit var viewBackward: AppCompatTextView
    private lateinit var tvPlayerEndTime: AppCompatTextView
    private lateinit var tvPlayerCurrentTime: AppCompatTextView
    private lateinit var videoBackward: AppCompatImageView
    private lateinit var videoForward: AppCompatImageView
    private lateinit var mProgressBar: CircularProgressBar


    private val timeRunnable: Runnable = object : Runnable {
        override fun run() {
            timeHandler.postDelayed(this, 1000)
            if (player == null) {
                return
            }
            val currentPosition = player!!.currentPosition
            tvPlayerCurrentTime.text = stringForTime(currentPosition.toInt())
            player?.duration?.let {
                progressBarBottom.max = it.toInt()
                tvPlayerEndTime.text = stringForTime(it.toInt())
            }
            if (currentPosition == lastPosition) return
            lastPosition = currentPosition
            progressBarBottom.progress = currentPosition.toInt()
            playerListener?.onCurrentTimeUpdated(lastPosition)
        }
    }

    private var gestureDetector = object : OnSwipeTouchListener(true) {
        var diffTime = -1f
        var finalTime = -1f
        var startVolume: Int = 0
        var maxVolume: Int = 0
        var startBrightness: Int = 0
        var maxBrightness: Int = 0

        override fun onMove(dir: Direction, diff: Float) {
            if (mGestureType != GestureType.SwipeGesture)
                return

            if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                player?.let { player ->
                    diffTime = if (player.duration <= 60) {
                        player.duration.toFloat() * diff / mInitialTextureWidth.toFloat()
                    } else {
                        60000.toFloat() * diff / mInitialTextureWidth.toFloat()
                    }
                    if (dir == Direction.LEFT) {
                        diffTime *= -1f
                    }
                    finalTime = player.currentPosition + diffTime
                    if (finalTime < 0) {
                        finalTime = 0f
                    } else if (finalTime > player.duration) {
                        finalTime = player.duration.toFloat()
                    }
                    diffTime = finalTime - player.currentPosition

                    val progressText = getUpDurationString(
                        finalTime.toLong(),
                        false
                    ) +
                            " [" + (if (dir == Direction.LEFT) "-" else "+") +
                            getUpDurationString(
                                Math.abs(diffTime).toLong(), false
                            ) +
                            "]"
                    mPositionTextView.text = progressText
                }
            } else {
                finalTime = -1f
                if (initialX >= mInitialTextureWidth / 2 || mWindow == null) {

                    var diffVolume: Float
                    var finalVolume: Int

                    diffVolume = maxVolume.toFloat() * diff / (mInitialTextureHeight.toFloat() / 2)
                    if (dir == Direction.DOWN) {
                        diffVolume = -diffVolume
                    }
                    finalVolume = startVolume + diffVolume.toInt()
                    if (finalVolume < 0)
                        finalVolume = 0
                    else if (finalVolume > maxVolume)
                        finalVolume = maxVolume

                    val progressText = String.format(
                        resources.getString(R.string.volume), finalVolume
                    )
                    mPositionTextView.text = progressText
                    am?.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolume, 0)
                    controllerAutoHideOnDelay()
                } else if (initialX < mInitialTextureWidth / 2) {

                    var diffBrightness: Float
                    var finalBrightness: Int

                    diffBrightness =
                        maxBrightness.toFloat() * diff / (mInitialTextureHeight.toFloat() / 2)
                    if (dir == Direction.DOWN) {
                        diffBrightness = -diffBrightness
                    }
                    finalBrightness = startBrightness + diffBrightness.toInt()
                    if (finalBrightness < 0)
                        finalBrightness = 0
                    else if (finalBrightness > maxBrightness)
                        finalBrightness = maxBrightness

                    val progressText = String.format(
                        resources.getString(R.string.brightness),
                        finalBrightness
                    )
                    mPositionTextView.text = progressText

                    val layout = mWindow?.attributes
                    layout?.screenBrightness = finalBrightness.toFloat() / 100
                    mWindow?.attributes = layout
                    controllerAutoHideOnDelay()
                }
            }
        }

        override fun onClick() {
            toggleControls()
        }

        override fun onDoubleTap(event: MotionEvent) {
            //if (mGestureType == GestureType.DoubleTapGesture) {
            val seekSec = mDoubleTapSeekDuration / 1000
            viewForward.text = String.format(
                resources.getString(R.string.seconds),
                seekSec
            )
            viewBackward.text = String.format(
                resources.getString(R.string.seconds),
                seekSec
            )
            if (event.x > mInitialTextureWidth / 2) {
                viewForward.let {
                    AppAnalytics.create(AnalyticsEvent.VIDEO_BTM_BTN.NAME)
                        .addParam("btn", "double tap forward").push()
                    animateViewFade(it, 1)
                    Handler().postDelayed({
                        animateViewFade(it, 0)
                    }, 500)
                }
                seekTo(getCurrentPosition() + mDoubleTapSeekDuration)
            } else {
                viewBackward.let {

                    AppAnalytics.create(AnalyticsEvent.VIDEO_BTM_BTN.NAME)
                        .addParam("btn", "double tap backward").push()
                    animateViewFade(it, 1)
                    Handler().postDelayed({
                        animateViewFade(it, 0)
                    }, 500)
                }
                seekTo(getCurrentPosition() - mDoubleTapSeekDuration)
            }
        }

        override fun onAfterMove() {
            if (finalTime >= 0 && mGestureType == GestureType.SwipeGesture) {
                currentPosition = finalTime.toLong()
                resumePlayer()
            }
            mPositionTextView.visibility = View.GONE
        }

        override fun onBeforeMove(dir: Direction) {
            if (mGestureType != GestureType.SwipeGesture)
                return
            if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                pausePlayer()
                mPositionTextView.visibility = View.VISIBLE
            } else {
                maxBrightness = 100
                mWindow?.attributes?.let {
                    startBrightness = (it.screenBrightness * 100).toInt()
                }
                maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
                startVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 100
                mPositionTextView.visibility = View.VISIBLE
            }
        }

        override fun lastTouchScreen(time: Long) {

        }
    }


    init {
        initPlayer()
        am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    }

    private fun initPlayer() {
        activity = context.activity()

        activity?.let {
            (it as LifecycleOwner).lifecycle.addObserver(this)
        }

        if (player == null) {
            try {
                val renderersFactory: RenderersFactory = getDefaultRenderersFactory()
                trackSelector?.parameters = ParametersBuilder(context).build()

                val trackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory()
                trackSelector = DefaultTrackSelector(context, trackSelectionFactory)
                trackSelector?.parameters?.buildUpon()?.apply {
                    setForceLowestBitrate(true)
                    setForceHighestSupportedBitrate(false)
                    setAllowAudioMixedChannelCountAdaptiveness(true)
                    setAllowAudioMixedMimeTypeAdaptiveness(true)
                    setAllowAudioMixedSampleRateAdaptiveness(true)
                }?.build()?.run {
                    trackSelector?.parameters = this
                }


                val defaultAllocator =
                    DefaultAllocator(true, DEFAULT_VIDEO_BUFFER_SIZE)

                val defaultLoadControl = DefaultLoadControl.Builder().setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                    .setAllocator(defaultAllocator)
                    .createDefaultLoadControl()

                player = SimpleExoPlayer.Builder(context, renderersFactory)
                    .setLoadControl(defaultLoadControl)
                    .setUseLazyPreparation(true)
                    .setTrackSelector(trackSelector!!).build()
            } catch (e: Exception) {
                throw e
            }
            setPlayer(player)
            setupAudioFocus()
            controllerAutoShow = true
            controllerHideOnTouch = true
            controllerShowTimeoutMs = 2500
            setControllerVisibilityListener(this)
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
            initView()
            initListener()
            player?.addListener(PlayerEventListener())
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            mControlsDisabled = false
            isClickable = true
            setOnTouchListener(gestureDetector)
            (videoSurfaceView as TextureView).surfaceTextureListener = this
            mWindow = context.activity()?.window
            hideTopBarView()
        }
    }

    private fun initView() {
        val mTextureFrame =
            LayoutInflater.from(context).inflate(R.layout.other_media_view, this, false)
        addView(mTextureFrame)
        defaultTimeBar = findViewById(R.id.exo_progress)
        defaultTimeBar.callOnClick()
        progressBarBottom = findViewById(R.id.progress_bar_bottom)
        imgFullScreenEnterExit = findViewById(R.id.img_full_screen_enter_exit)
        mPositionTextView = findViewById(R.id.position_text_view)
        viewForward = findViewById(R.id.view_forward)
        viewBackward = findViewById(R.id.view_backward)
        tvPlayerEndTime = findViewById(R.id.tv_player_end_time)
        tvPlayerCurrentTime = findViewById(R.id.tv_player_current_time)
        videoBackward = findViewById(R.id.img_bwd)
        videoForward = findViewById(R.id.img_fwd)
        mProgressBar = findViewById(R.id.progress_bar)
    }

    fun getToolbar() = mToolbar
    fun setToolbar(toolbar: Toolbar) {
        this.mToolbar = toolbar
    }

    fun hideFullScreenController() {
        imgFullScreenEnterExit.visibility = View.GONE
    }


    private fun initListener() {
        imgFullScreenEnterExit.setOnClickListener {
            val display =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val orientation = display.rotation
            if (orientation == Surface.ROTATION_90 || orientation == Surface.ROTATION_270) {
                imgFullScreenEnterExit.setImageResource(R.drawable.ic_full_screen_enter)
                context.activity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

            } else {
                imgFullScreenEnterExit.setImageResource(R.drawable.ic_full_screen_exit)
                context.activity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

            }
            playerListener?.onClickFullScreenView(
                context.activity()?.requestedOrientation
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            )
        }
        defaultTimeBar.addListener(object : OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                playerListener?.onPositionDiscontinuity(player?.currentPosition ?: lastPosition)
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {

            }

            override fun onScrubStop(
                timeBar: TimeBar,
                position: Long,
                canceled: Boolean
            ) {
            }
        })
        videoBackward.setOnClickListener {

            AppAnalytics.create(AnalyticsEvent.VIDEO_BTM_BTN.NAME)
                .addParam("btn", "10 sec forward").push()
            seekTo(getCurrentPosition() - 10 * 1000)
        }
        videoForward.setOnClickListener {

            AppAnalytics.create(AnalyticsEvent.VIDEO_BTM_BTN.NAME)
                .addParam("btn", "10 sec backward").push()
            seekTo(getCurrentPosition() + 10 * 1000)
        }
    }

    private fun setupAudioFocus() {
        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build()
        player!!.setAudioAttributes(audioAttributes, true)
    }

    private tailrec fun Context?.activity(): Activity? = when (this) {
        is Activity -> this
        else -> (this as? ContextWrapper)?.baseContext?.activity()
    }


    override fun onVisibilityChange(visibility: Int) {
        if (visibility == View.GONE) {
            progressBarBottom.visibility = View.VISIBLE
            hideToolbarWithAnimation()
        } else {
            showToolbar()
            progressBarBottom.visibility = View.GONE
        }
    }

    fun toggleControls() {
        if (mControlsDisabled)
            return

        if (isControllerVisible) {
            val controller = findViewById<View>(R.id.exo_controller)
            controller.animate().alpha(0f)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        controller.visibility = View.GONE
                        hideController()
                    }
                }).start()
            hideToolbarWithAnimation()
            progressBarBottom.visibility = View.VISIBLE


        } else {
            val controller = findViewById<View>(R.id.exo_controller)
            controller.visibility = View.VISIBLE
            controller?.animate()?.alpha(1f)?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    controller.visibility = View.VISIBLE
                    showController()
                }
            })
                ?.setInterpolator(DecelerateInterpolator())?.start()
            showToolbar()
            progressBarBottom.visibility = View.GONE

        }
    }

    private fun showToolbar() {
        mToolbar?.run {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            this.animate()?.cancel()
            this.alpha = 0f
            this.visibility = View.VISIBLE
            this.animate()?.alpha(1f)?.setListener(null)
                ?.setInterpolator(DecelerateInterpolator())?.start()
        }
    }

    private fun hideToolbarWithAnimation() {
        mToolbar?.let { toolbar ->
            if (toolbar.visibility == View.VISIBLE) {
                toolbar.animate().cancel()
                toolbar.alpha = 1f
                toolbar.animate().alpha(0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            toolbar.visibility = View.GONE
                            hideTopBarView()
                        }
                    }).start()
            }
        }
    }

    private fun animateViewFade(view: View, alpha: Int) {
        val viewVisibility = if (alpha > 0) View.VISIBLE else View.INVISIBLE
        view.animate()
            .alpha(alpha.toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = viewVisibility
                }
            })
    }

    private fun hideTopBarView() {
        if (mToolbar != null) {
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    fun setVideoPlayerEventListener(playerListener: VideoPlayerEventListener?) {
        this.playerListener = playerListener
    }

    fun setUrl(url: String?) {
        uri = Uri.parse(url)
    }

    fun playVideo() {
        player?.playWhenReady = true
        player?.playbackState
        uri?.let {
            player!!.prepare(VideoDownloadController.getInstance().getMediaSource(uri), true, false)
        }
        seekTo(lastPosition)
        timeHandler.post(timeRunnable)
    }

    private fun playVideoInternal() {
        player?.playWhenReady = true
        player?.playbackState
        uri?.let {
            player!!.prepare(VideoDownloadController.getInstance().getMediaSource(uri), true, false)
        }
        seekTo(lastPosition)
        timeHandler.post(timeRunnable)
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onStartPlayer() {
        if (player == null) {
            initPlayer()
            playVideoInternal()
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResumePlayer() {
        onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        pausePlayer()
        onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopPlayerEvent() {
        releasePlayer()
    }

    private fun releasePlayer() {
        playerListener?.onPlayerReleased()
        if (player != null) {
            player!!.release()
            player = null
            trackSelector = null
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        activity?.let {
            (it as LifecycleOwner).lifecycle.removeObserver(this)
        }
    }


    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    fun pausePlayer() {
        player?.run {
            playWhenReady = false
            playbackState
            this@BitVideoPlayer.currentPosition = currentPosition
        }
    }

    fun resumePlayer() {
        player?.run {
            playWhenReady = true
            playbackState
            seekTo(this@BitVideoPlayer.currentPosition)
        }
    }

    private fun getDefaultRenderersFactory(): DefaultRenderersFactory {
        val renderersFactory = DefaultRenderersFactory(context)
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        return renderersFactory
    }


    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        mInitialTextureWidth = width
        mInitialTextureHeight = height
        mSurfaceAvailable = true
        mSurface = Surface(surfaceTexture)
        mSurface?.run {
            player?.setVideoSurface(mSurface)
        }
    }


    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        mSurfaceAvailable = false
        mSurface = null
        return false
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: -1
    }

    fun seekTo(pos: Long) {
        player?.seekTo(pos)
        controllerAutoHideOnDelay()
    }

    private fun controllerAutoHideOnDelay() {
        controllerHandler.removeCallbacksAndMessages(null)
        controllerHandler.postDelayed({
            if (isControllerVisible) {
                toggleControls()
            }
        }, 3500)
    }


    fun openVideoPlayerOptions() {
        val optionOfVideoPlayer = arrayListOf<VideoPlayerOption>()
        optionOfVideoPlayer.add(VideoPlayerOption.Quality(extraInfo = videoQualityP))
        optionOfVideoPlayer.add(VideoPlayerOption.AudioLanguage(extraInfo = audioLanguage))
        optionOfVideoPlayer.add(VideoPlayerOption.PlaybackSpeed(extraInfo = playbackSpeedTitle))
        optionOfVideoPlayer.add(VideoPlayerOption.Help(extraInfo = ""))

        activity?.run {
            val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
            val view =
                LayoutInflater.from(context)
                    .inflate(R.layout.base_recycler_view_layout, this@BitVideoPlayer, false)
            val dialog = MaterialDialog(this, bottomSheet).show {
                customView(view = view)
                setFinishOnTouchOutside(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                }
            }
            val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = VideoOptionAdapter(optionOfVideoPlayer,
                object : VideoOptionAdapter.OnVideoOptionListener {
                    override fun onSelect(videoPlayerOption: VideoPlayerOption) {
                        bottomSheet.onDismiss()
                        dialog.dismiss()
                        when (videoPlayerOption) {
                            is VideoPlayerOption.Quality -> {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE.NAME)
                                    .addParam("btn", "Quality").push()
                                openVideoTrackBottomBar()
                            }
                            is VideoPlayerOption.AudioLanguage -> {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE.NAME)
                                    .addParam("btn", "language").push()
                                openAudioLanguageTrackOption()
                            }
                            is VideoPlayerOption.PlaybackSpeed -> {
                                openPlaybackSpeedOption()
                            }
                            is VideoPlayerOption.Help -> {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE.NAME)
                                    .addParam("btn", "Help").push()
                                playerListener?.helpAndFeedback()
                            }

                        }
                    }
                })
        }
    }


    fun openVideoTrackBottomBar() {
        activity?.run {
            val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
            val view =
                LayoutInflater.from(context)
                    .inflate(R.layout.base_recycler_view_layout, null, false)
            val dialog = MaterialDialog(this, bottomSheet).show {
                customView(view = view)
                setFinishOnTouchOutside(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                }
            }
            val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = VideoTrackAdapter(lisOfVideoQualityTrack,
                object : VideoTrackAdapter.OnSelectQualityListener {
                    override fun onSelect(videoQualityTrack: VideoQualityTrack) {
                        bottomSheet.onDismiss()
                        dialog.dismiss()
                        onSelectTrack(videoQualityTrack)
                    }
                })
        }
    }

    fun openAudioLanguageTrackOption() {
        activity?.run {
            val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
            val view =
                LayoutInflater.from(context)
                    .inflate(R.layout.base_recycler_view_layout, null, false)
            val dialog = MaterialDialog(this, bottomSheet).show {
                customView(view = view)
                setFinishOnTouchOutside(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                }
            }
            val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = AudioLanguageAdapter(lisOfAudioLanguageTrack,
                object : AudioLanguageAdapter.OnSelectQualityListener {
                    override fun onSelect(audioLanguageTrack: AudioLanguageTrack) {
                        bottomSheet.onDismiss()
                        dialog.dismiss()
                        onSelectAudioTrack(audioLanguageTrack)
                    }
                })

        }
    }


    @SuppressLint("InflateParams")
    fun openPlaybackSpeedOption() {
        val optionOfSpeedLayer = arrayListOf<PlaybackSpeed>()
        optionOfSpeedLayer.add(PlaybackSpeed(1, 0.25F, "0.25x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(2, 0.50F, "0.55x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(3, 0.75F, "0.75x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(4, 1F, "Normal", false))
        optionOfSpeedLayer.add(PlaybackSpeed(5, 1.25F, "1.25x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(6, 1.5F, "1.50x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(7, 1.75F, "1.75x", false))
        optionOfSpeedLayer.add(PlaybackSpeed(8, 2.0F, "2x", false))
        optionOfSpeedLayer.map { t ->
            if (t.speed == playbackSpeed) {
                t.isSelected = true
            }
        }

        activity?.run {
            val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
            val view =
                LayoutInflater.from(context)
                    .inflate(R.layout.base_recycler_view_layout, null, false)
            val dialog = MaterialDialog(this, bottomSheet).show {
                customView(view = view)
                setFinishOnTouchOutside(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                }
            }
            val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager
            recyclerView.setHasFixedSize(false)
            recyclerView.adapter = PlaybackSpeedAdapter(
                optionOfSpeedLayer,
                object : PlaybackSpeedAdapter.OnPlaybackSpeedListener {
                    override fun onSelect(playbackSpeed: PlaybackSpeed) {
                        AppAnalytics.create(AnalyticsEvent.VIDEO_MORE.NAME)
                            .addParam("btn", "PlayBackSpeed ${playbackSpeed.speed}").push()
                        bottomSheet.onDismiss()
                        dialog.dismiss()
                        onChangePlaybackSpeed(playbackSpeed)
                    }
                })
        }
    }


    enum class GestureType {
        NoGesture, SwipeGesture, DoubleTapGesture
    }

    fun stringForTime(timeMs: Int): String {
        if (timeMs <= 0 || timeMs >= 24 * 60 * 60 * 1000) {
            return "00:00"
        }
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val stringBuilder = StringBuilder()
        val mFormatter =
            Formatter(stringBuilder, Locale.getDefault())
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }


    fun onSelectTrack(videoQualityTrack: VideoQualityTrack) {
        val parametersBuilder = trackSelector?.buildUponParameters()
       // parametersBuilder?.setRendererDisabled(0, false)
        val trackGroups = trackSelector!!.currentMappedTrackInfo!!.getTrackGroups(0)
        val selectionOverride: DefaultTrackSelector.SelectionOverride =
            DefaultTrackSelector.SelectionOverride(0, videoQualityTrack.id)
        parametersBuilder?.setSelectionOverride(0, trackGroups, selectionOverride)
        trackSelector!!.parameters = parametersBuilder!!.build()
        videoQualityP = videoQualityTrack.title
    }

    private fun onSelectAudioTrack(audioLanguageTrack: AudioLanguageTrack) {
        try {
            val parametersBuilder = trackSelector?.buildUponParameters()
            parametersBuilder?.setPreferredTextLanguage(audioLanguageTrack.id)
            parametersBuilder?.setPreferredAudioLanguage(audioLanguageTrack.id)
            trackSelector!!.parameters = parametersBuilder!!.build()
            audioLanguage = audioLanguageTrack.id
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun onChangePlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        this.playbackSpeed = playbackSpeed.speed
        this.playbackSpeedTitle = playbackSpeed.title
        val param = PlaybackParameters(this.playbackSpeed)
        player?.setPlaybackParameters(param)
    }


    private inner class PlayerErrorMessageProvider :
        ErrorMessageProvider<ExoPlaybackException> {
        override fun getErrorMessage(e: ExoPlaybackException): Pair<Int, String> {
            e.printStackTrace()
            return Pair.create(0, "errorString")
        }
    }

    private inner class PlayerEventListener : Player.EventListener {
        var playWhenReady = false
        var playbackState: Int = STATE_IDLE

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            super.onTracksChanged(trackGroups, trackSelections)
            try {
                audioLanguage = trackSelections[1]?.selectedFormat?.language ?: ""
                val oldTrack = currentMappedTrackInfoPosition
                //trackSelections[0]?.selectedFormat.width
                currentMappedTrackInfoPosition =
                    trackSelections.get(0)?.getIndexInTrackGroup(0) ?: 0
                if (lisOfVideoQualityTrack.isNullOrEmpty() || oldTrack != currentMappedTrackInfoPosition) {
                    lisOfVideoQualityTrack.clear()
                    val mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? =
                        trackSelector?.currentMappedTrackInfo

                    if (mappedTrackInfo != null) {
                        val trackGroups =
                            trackSelector!!.currentMappedTrackInfo!!.getTrackGroups(0)[0] //render index important
                        for (x in 0 until trackGroups.length) {
                            val currentQuality = "" + trackGroups.getFormat(x).height + "p"
                            lisOfVideoQualityTrack.add(
                                VideoQualityTrack(
                                    x,
                                    trackGroups.getFormat(x).height,
                                     currentQuality, currentMappedTrackInfoPosition == x
                                )
                            )
                        }
                    }
                    lisOfVideoQualityTrack.sortBy { it.quality }
                }
                if (videoQualityP.isEmpty()) {
                    videoQualityP = lisOfVideoQualityTrack[0].title
                }
                if (lisOfAudioLanguageTrack.isNullOrEmpty()) {

                    val mappedTrackInfo: MappingTrackSelector.MappedTrackInfo? =
                        trackSelector?.currentMappedTrackInfo
                    if (mappedTrackInfo != null) {
                        val infoArray = trackSelector!!.currentMappedTrackInfo!!.getTrackGroups(1)
                        for (x in 0 until infoArray.length) {
                            val language = infoArray.get(x).getFormat(0).language
                            lisOfAudioLanguageTrack.add(
                                AudioLanguageTrack(
                                    language ?: "",
                                    language ?: "",
                                    currentMappedTrackInfoPosition == x
                                )
                            )
                        }
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }

        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            error.printStackTrace()
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            super.onLoadingChanged(isLoading)
            if (isLoading.not()) {
                //  mProgressBar.visibility = View.GONE
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            this.playWhenReady = playWhenReady
            this.playbackState = playbackState

            if (playbackState == Player.STATE_READY) {
                playerListener?.onPlayerReady()
            }

            playerListener?.onPlayerStateChanged(playWhenReady, playbackState)
            val state = when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> "buffering"
                ExoPlayer.STATE_ENDED -> "ended"
                ExoPlayer.STATE_READY -> "ready"
                ExoPlayer.STATE_IDLE -> "idle"
                else -> "unknownState$playbackState"
            }
            if (playbackState == STATE_BUFFERING) {
                mProgressBar.visibility = View.VISIBLE
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mWasPlaying = isPlaying
            if (isPlaying) {
                timeHandler.postDelayed({
                    mProgressBar.visibility = View.GONE
                }, 500)

            } else {
                if (playbackState == STATE_BUFFERING) {
                    mProgressBar.visibility = View.VISIBLE
                }
            }

        }
    }
}


sealed class VideoPlayerOption(
    val order: Int,
    val rId: Int,
    val name: String,
    val extraInfo: String?
) {
    class Quality(
        order: Int = 1,
        rId: Int = R.drawable.ic_baseline_settings,
        name: String = "Quality &nbsp; &#8226; &nbsp;  ",
        extraInfo: String? = null
    ) : VideoPlayerOption(order, rId, name, extraInfo)

    class AudioLanguage(
        order: Int = 2,
        rId: Int = R.drawable.ic_baseline_language,
        name: String = "Language &nbsp; &#8226; &nbsp; ",
        extraInfo: String? = null
    ) : VideoPlayerOption(order, rId, name, extraInfo)

    class PlaybackSpeed(
        order: Int = 3,
        rId: Int = R.drawable.ic_baseline_playback,
        name: String = "Playback speed &nbsp; &#8226; &nbsp; ",
        extraInfo: String? = null
    ) : VideoPlayerOption(order, rId, name, extraInfo)

    class Help(
        order: Int = 4,
        rId: Int = R.drawable.ic_help_player,
        name: String = "Help & feedback",
        extraInfo: String? = null
    ) : VideoPlayerOption(order, rId, name, extraInfo)
}


/** Video Video Option adapter*/

class VideoOptionAdapter(
    private var items: List<VideoPlayerOption>,
    private var onVideoOptionListener: OnVideoOptionListener
) :
    RecyclerView.Adapter<VideoOptionAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.base_video_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.labelTextView.text = HtmlCompat.fromHtml(
            items[position].name.plus(items[position].extraInfo),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        holder.labelTextView.setCompoundDrawablesWithIntrinsicBounds(items[position].rId, 0, 0, 0)
        holder.labelTextView.compoundDrawables[0]?.setTint(
            ContextCompat.getColor(
                holder.itemView.context,
                R.color.gray_9E
            )
        )

        holder.itemView.setOnClickListener {
            onVideoOptionListener.onSelect(items[position])
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var labelTextView: MaterialTextView = view.findViewById(R.id.text_view)
    }

    interface OnVideoOptionListener {
        fun onSelect(videoPlayerOption: VideoPlayerOption)
    }
}

/**end **/


/** Video Playback speed  adapter*/

data class PlaybackSpeed(val id: Int, val speed: Float, val title: String, var isSelected: Boolean)

class PlaybackSpeedAdapter(
    private var items: List<PlaybackSpeed>,
    private var onPlaybackSpeedListener: OnPlaybackSpeedListener
) :
    RecyclerView.Adapter<PlaybackSpeedAdapter.ViewHolder>() {
    private var selectedPos: Int = items.indexOfLast { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.base_video_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.labelTextView.text = items[position].title
        holder.labelTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
        if (selectedPos == position) {
            holder.labelTextView.compoundDrawables[0]?.setTint(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.gray_79
                )
            )
        }
        holder.itemView.setOnClickListener {
            onPlaybackSpeedListener.onSelect(items[position])
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var labelTextView: MaterialTextView = view.findViewById(R.id.text_view)
    }

    interface OnPlaybackSpeedListener {
        fun onSelect(playbackSpeed: PlaybackSpeed)
    }
}

/**end **/


/** Video VideoQualityTrack adapter*/

data class VideoQualityTrack(
    val id: Int,
    val quality: Int,
    val title: String,
    val isSelected: Boolean
)

class VideoTrackAdapter(
    private var items: List<VideoQualityTrack> = emptyList(),
    private var onSelectQualityListener: OnSelectQualityListener
) :
    RecyclerView.Adapter<VideoTrackAdapter.ViewHolder>() {

    private var selectedPos: Int = items.indexOfLast { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.base_video_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.labelTextView.text = items[position].title
        holder.itemView.setOnClickListener {
            onSelectQualityListener.onSelect(items[position])
        }
        holder.labelTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
        if (selectedPos == position) {
            holder.labelTextView.compoundDrawables[0]?.setTint(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.gray_79
                )
            )
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var labelTextView: MaterialTextView = view.findViewById(R.id.text_view)
    }

    interface OnSelectQualityListener {
        fun onSelect(videoQualityTrack: VideoQualityTrack)
    }
}
/*end **/


/* start audio **/



data class AudioLanguageTrack(val id: String, val title: String, val isSelected: Boolean)
class AudioLanguageAdapter(
    private var items: List<AudioLanguageTrack> = emptyList(),
    private var onSelectQualityListener: OnSelectQualityListener
) :
    RecyclerView.Adapter<AudioLanguageAdapter.ViewHolder>() {

    private var selectedPos: Int = items.indexOfLast { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.base_video_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.labelTextView.text = items[position].title
        holder.itemView.setOnClickListener {
            onSelectQualityListener.onSelect(items[position])
        }
        holder.labelTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
        if (selectedPos == position) {
            holder.labelTextView.compoundDrawables[0]?.setTint(Color.BLUE)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var labelTextView: MaterialTextView = view.findViewById(R.id.text_view)
    }

    interface OnSelectQualityListener {
        fun onSelect(audioLanguageTrack: AudioLanguageTrack)
    }
}

/******end **/

interface VideoPlayerEventListener {
    fun onClickFullScreenView(cOrientation: Int)
    fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int)
    fun onCurrentTimeUpdated(time: Long)
    fun onPlayerReleased()
    fun onPositionDiscontinuity(lastPos: Long, reason: Int = 1)
    fun onPlayerReady()
    fun helpAndFeedback()

}


fun getUpDurationString(durationMs: Long, negativePrefix: Boolean): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)

    return if (hours > 0) {
        java.lang.String.format(
            Locale.getDefault(), "%s%02d:%02d:%02d",
            if (negativePrefix) "-" else "",
            hours,
            minutes - TimeUnit.HOURS.toMinutes(hours),
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    } else java.lang.String.format(
        Locale.getDefault(), "%s%02d:%02d",
        if (negativePrefix) "-" else "",
        minutes,
        seconds - TimeUnit.MINUTES.toSeconds(minutes)
    )
}
/**end **/

/*

sealed class Either<out L, out R>
class Left<L>: Either<L, Nothing>()
class Right<R>: Either<Nothing, R>()*/
