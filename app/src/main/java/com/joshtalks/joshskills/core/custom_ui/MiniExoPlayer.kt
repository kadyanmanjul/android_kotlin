package com.joshtalks.joshskills.core.custom_ui


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_VIDEO_BUFFER_SIZE
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlaybackException.TYPE_SOURCE
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import java.util.*
import kotlin.collections.HashMap

class MiniExoPlayer : PlayerView, LifecycleObserver, PlayerControlView.VisibilityListener {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        @JvmStatic
        var duration: HashMap<String, Long> = HashMap()
    }

    private var uri: Uri? = null
    private var videoId: Int? = null
    private var lastPosition: Long = 0
    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private val timeHandler = Handler()
    private val controllerHandler = Handler()
    private var tag: String = EMPTY
    private var am: AudioManager? = null
    private var mControlsDisabled = false
    private var currentPosition: Long = 0
    private var mIsPlaying: Boolean = false
    private var activity: Activity? = null
    private var playerListener: VideoPlayerEventListener? = null
    private lateinit var defaultTimeBar: DefaultTimeBar
    private lateinit var progressBarBottom: ProgressBar
    private lateinit var imgFullScreenEnterExit: AppCompatImageView
    private lateinit var tvPlayerEndTime: AppCompatTextView
    private lateinit var tvPlayerCurrentTime: AppCompatTextView
    private lateinit var placeHolderImageView: AppCompatImageView
    private var mIsPlayerInit: Boolean = false


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
                defaultTimeBar.setDuration(it)
            }
            if (currentPosition == lastPosition) return
            lastPosition = currentPosition
            progressBarBottom.progress = currentPosition.toInt()
            defaultTimeBar.setPosition(currentPosition)
            playerListener?.onCurrentTimeUpdated(lastPosition)
            duration[tag] = lastPosition
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
                e.printStackTrace()
            }
            setPlayer(player)
            setupAudioFocus()
            controllerAutoShow = true
            controllerHideOnTouch = true
            setControllerVisibilityListener(this)
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
            initView()
            initListener()
            player?.addListener(PlayerEventListener())
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            mControlsDisabled = false
            isClickable = true
            setKeepContentOnPlayerReset(true)
        }
    }

    private fun initView() {
        val mTextureFrame =
            LayoutInflater.from(context).inflate(R.layout.mini_player_media_view, this, false)
        addView(mTextureFrame)
        placeHolderImageView = findViewById(R.id.exo_artwork)
        defaultTimeBar = findViewById(R.id.exo_progress)
        defaultTimeBar.callOnClick()
        progressBarBottom = findViewById(R.id.progress_bar_bottom)
        imgFullScreenEnterExit = findViewById(R.id.img_full_screen_enter_exit)
        tvPlayerEndTime = findViewById(R.id.tv_player_end_time)
        tvPlayerCurrentTime = findViewById(R.id.tv_player_current_time)
        findViewById<View>(R.id.exo_buffering).visibility = View.GONE


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
        findViewById<View>(R.id.exo_play).setOnClickListener {
            if (mIsPlayerInit) {
                resumePlayer()
            } else {
                initVideo()
            }
        }
        findViewById<View>(R.id.exo_pause).setOnClickListener {
            pausePlayer()
        }

        findViewById<View>(R.id.img_bwd).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.VIDEO_ACTION.NAME)
                .addParam(AnalyticsEvent.VIDEO_ACTION.NAME, "10 sec forward").push()
            seekTo(getCurrentPosition() - 10 * 1000)
        }
        findViewById<View>(R.id.img_fwd).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.VIDEO_ACTION.NAME)
                .addParam(AnalyticsEvent.VIDEO_ACTION.NAME, "10 sec backward").push()
            seekTo(getCurrentPosition() + 10 * 1000)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pausePlayer()
    }

    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: -1
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
            if (mIsPlaying.not()) {
                //  visiblePlayButton()
            }
            progressBarBottom.visibility = View.VISIBLE
        } else {
            progressBarBottom.visibility = View.GONE
            if (mIsPlaying) {
                //   hidePlayButton()
            }
        }
    }

    fun setUrl(url: String?, placeHolderUrl: String? = null, remoteId: Int) {
        videoId=remoteId
        uri = Uri.parse(url)
        if (placeHolderUrl.isNullOrEmpty().not()) {
            setPlaceHolder(placeHolderUrl!!)
        }
        url?.let {
            this.tag = it
        }
    }

    private fun setPlaceHolder(url: String) {
        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(placeHolderImageView)
    }

    private fun initVideo() {
        uri?.let {
            player!!.prepare(VideoDownloadController.getInstance().getMediaSource(uri))
            logVideoPlayedEvent(false)
        }
        player?.playWhenReady = true
        player?.playbackState
        duration[tag]?.run {
            lastPosition = this
            seekTo(lastPosition)
        }
        timeHandler.post(timeRunnable)
    }

    private fun logVideoPlayedEvent(videoResumed: Boolean) {
        AppAnalytics.create(AnalyticsEvent.ASSESSMENT_VIDEO_PLAYED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.VIDEO_ID.NAME,videoId.toString())
            .addParam("video_resumed",videoResumed)
            .push()
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
        duration.clear()
        activity?.let {
            (it as LifecycleOwner).lifecycle.removeObserver(this)
        }
    }


    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    private fun pausePlayer() {
        player?.run {
            playWhenReady = false
            this@MiniExoPlayer.currentPosition = currentPosition
        }
    }

    private fun resumePlayer() {
        player?.run {
            logVideoPlayedEvent(true)
            playWhenReady = true
            playbackState
        }
    }

    private fun getDefaultRenderersFactory(): DefaultRenderersFactory {
        val renderersFactory = DefaultRenderersFactory(context)
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        return renderersFactory
    }


    fun seekTo(pos: Long) {
        player?.seekTo(pos)
        controllerAutoHideOnDelay()
    }

    private fun controllerAutoHideOnDelay() {
        controllerHandler.removeCallbacksAndMessages(null)
        controllerHandler.postDelayed({
        }, 3500)
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

        override fun onPlayerError(error: ExoPlaybackException) {
            super.onPlayerError(error)
            error.printStackTrace()
            if (error.type == TYPE_SOURCE) {
                mIsPlayerInit = false

            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            this.playWhenReady = playWhenReady
            this.playbackState = playbackState

            if (playbackState == Player.STATE_READY) {
                playerListener?.onPlayerReady()
                if (mIsPlayerInit.not()) {
                    mIsPlayerInit = true
                }
            }
            if (playbackState == Player.STATE_ENDED) {
                player?.playWhenReady = false
                player?.seekTo(0, 0)
            }


            playerListener?.onPlayerStateChanged(playWhenReady, playbackState)
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> "buffering"
                ExoPlayer.STATE_ENDED -> "ended"
                ExoPlayer.STATE_READY -> "Ready"
                ExoPlayer.STATE_IDLE -> "idle"
                else -> "unknownState$playbackState"
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mIsPlaying = isPlaying
        }

    }
}
