package com.joshtalks.joshskills.core.custom_ui


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_VIDEO_BUFFER_SIZE
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.util.*

class MiniExoPlayer : PlayerView, LifecycleObserver, PlayerControlView.VisibilityListener {

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

    private var am: AudioManager? = null
    private var mControlsDisabled = false
    private var currentPosition: Long = 0
    private var mIsPlaying: Boolean = false
    private var currentMappedTrackInfoPosition: Int = 0
    private var activity: Activity? = null
    private var lisOfVideoQualityTrack = mutableListOf<VideoQualityTrack>()
    private var videoQualityP: String = ""
    private var playbackSpeed: Float = 1F
    private var playbackSpeedTitle: String = "Normal"
    private var audioLanguage: String = ""
    private var lisOfAudioLanguageTrack = mutableListOf<AudioLanguageTrack>()
    private var playerListener: VideoPlayerEventListener? = null

    private lateinit var defaultTimeBar: DefaultTimeBar
    private lateinit var progressBarBottom: ProgressBar
    private lateinit var imgFullScreenEnterExit: AppCompatImageView
    private lateinit var tvPlayerEndTime: AppCompatTextView
    private lateinit var tvPlayerCurrentTime: AppCompatTextView
    private lateinit var mProgressBar: CircularProgressBar
    private lateinit var ivPlayPause: AppCompatImageView


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
            controllerAutoShow = false
            controllerHideOnTouch = true
            controllerShowTimeoutMs = 2500
            setControllerVisibilityListener(this)
            setErrorMessageProvider(PlayerErrorMessageProvider())
            requestFocus()
            initView()
            initListener()
            player?.addListener(PlayerEventListener())
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            mControlsDisabled = false
            isClickable = true
        }
    }

    private fun initView() {
        val mTextureFrame =
            LayoutInflater.from(context).inflate(R.layout.mini_player_media_view, this, false)
        addView(mTextureFrame)
        defaultTimeBar = findViewById(R.id.exo_progress)
        defaultTimeBar.callOnClick()
        progressBarBottom = findViewById(R.id.progress_bar_bottom)
        imgFullScreenEnterExit = findViewById(R.id.img_full_screen_enter_exit)
        tvPlayerEndTime = findViewById(R.id.tv_player_end_time)
        tvPlayerCurrentTime = findViewById(R.id.tv_player_current_time)
        mProgressBar = findViewById(R.id.progress_bar)
        ivPlayPause = findViewById(R.id.play_pause_btn)
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
        ivPlayPause.setOnClickListener {
            if (mIsPlaying) {
                pausePlayer()
            } else {
                resumePlayer()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.e("onDetachedFromWindow", "MiniExoplayer")

        pausePlayer()

    }

    private fun hidePlayButton() {
        ivPlayPause.animate()?.setStartDelay(1000)?.alpha(0f)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ivPlayPause.visibility = View.GONE

                }
            })?.setInterpolator(DecelerateInterpolator())?.start()
    }

    private fun visiblePlayButton() {
        if (isControllerVisible) {
            return
        }
        ivPlayPause.animate()?.setStartDelay(500)?.alpha(1f)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ivPlayPause.visibility = View.VISIBLE
                }
            })?.setInterpolator(DecelerateInterpolator())?.start()
    }

    private fun playPauseState() {
        if (mIsPlaying) {
            ivPlayPause.setImageResource(R.drawable.ic_pause_notification)
            hidePlayButton()
        } else {
            ivPlayPause.setImageResource(R.drawable.ic_play_notification)
            visiblePlayButton()
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
            if (mIsPlaying.not()) {
                visiblePlayButton()
            }
            progressBarBottom.visibility = View.VISIBLE
        } else {
            progressBarBottom.visibility = View.GONE
            if (mIsPlaying) {
                hidePlayButton()
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


    fun setVideoPlayerEventListener(playerListener: com.joshtalks.joshskills.core.videoplayer.VideoPlayerEventListener) {
        this.playerListener = playerListener
    }

    fun setUrl(url: String?) {
        uri = Uri.parse(url)
    }

    fun initVideo() {
        player?.playWhenReady = false
        uri?.let {
            player!!.prepare(
                VideoDownloadController.getInstance().getMediaSource(uri),
                false,
                false
            )
        }
        seekTo(lastPosition)
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
            this@MiniExoPlayer.currentPosition = currentPosition
        }
    }

    private fun resumePlayer() {
        player?.run {
            playWhenReady = true
            playbackState
            seekTo(this@MiniExoPlayer.currentPosition)
        }
    }

    private fun getDefaultRenderersFactory(): DefaultRenderersFactory {
        val renderersFactory = DefaultRenderersFactory(context)
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        return renderersFactory
    }

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
                    .inflate(R.layout.base_recycler_view_layout, this@MiniExoPlayer, false)
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
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE_ACTIONS.NAME)
                                    .addParam(
                                        AnalyticsEvent.VIDEO_ACTION.NAME,
                                        "Change Video Quality"
                                    ).push()
                                openVideoTrackBottomBar()
                            }
                            is VideoPlayerOption.AudioLanguage -> {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE_ACTIONS.NAME)
                                    .addParam(AnalyticsEvent.VIDEO_ACTION.NAME, "Change language")
                                    .push()
                                openAudioLanguageTrackOption()
                            }
                            is VideoPlayerOption.PlaybackSpeed -> {
                                openPlaybackSpeedOption()
                            }
                            is VideoPlayerOption.Help -> {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_MORE_ACTIONS.NAME)
                                    .addParam(AnalyticsEvent.VIDEO_ACTION.NAME, "Help clicked")
                                    .push()
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
                        AppAnalytics.create(AnalyticsEvent.VIDEO_MORE_ACTIONS.NAME)
                            .addParam(
                                AnalyticsEvent.VIDEO_ACTION.NAME,
                                "PlayBackSpeed ${playbackSpeed.speed}"
                            ).push()
                        bottomSheet.onDismiss()
                        dialog.dismiss()
                        onChangePlaybackSpeed(playbackSpeed)
                    }
                })
        }
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
            when (playbackState) {
                ExoPlayer.STATE_BUFFERING -> "buffering"
                ExoPlayer.STATE_ENDED -> "ended"
                ExoPlayer.STATE_READY -> "Ready"
                ExoPlayer.STATE_IDLE -> "idle"
                else -> "unknownState$playbackState"
            }
            if (playbackState == STATE_BUFFERING) {
                mProgressBar.visibility = View.VISIBLE

            } else {
                mProgressBar.visibility = View.GONE
            }

        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            mIsPlaying = isPlaying
            if (isPlaying) {
                timeHandler.postDelayed({
                    mProgressBar.visibility = View.GONE
                }, 500)

            } else {
                if (playbackState == STATE_BUFFERING) {
                    mProgressBar.visibility = View.VISIBLE
                }
            }
            playPauseState()
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
