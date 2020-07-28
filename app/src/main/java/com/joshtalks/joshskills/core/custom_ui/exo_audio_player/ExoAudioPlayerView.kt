package com.joshtalks.joshskills.core.custom_ui.exo_audio_player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.R
import java.util.*

class ExoAudioPlayerView : FrameLayout, LifecycleObserver {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val tag: String = "ExoAudioPlayerView"
    private var playerControlView: PlayerControlView
    private lateinit var durationTv: TextView
    private lateinit var positionTv: TextView
    private var playerListener: AudioPlayerEventListener? = null
    private var player: SimpleExoPlayer? = null
    private var audioModels: LinkedList<AudioModel> = LinkedList()

    private var totalTime: Long = 0
    private var activity: Activity? = null
    private var lastPosition: Long = 0

    private val playerEventListener: Player.EventListener = object : Player.EventListener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (player!!.currentTag != null)
                playerListener?.onTrackChange(player?.currentTag as String?)
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            Log.e("audioo", "onLoadingChanged")
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            println("$tag state changed $playbackState")
            if (playbackState == Player.STATE_ENDED) {
                player?.playWhenReady = false
                player?.seekTo(0, 0)

            }
            Log.e("audioo", "onPlayerStateChanged" + playWhenReady + " " + playbackState)

        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            Log.e("audioo", "onPlaybackSuppressionReasonChanged")

        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying || player!!.playWhenReady) {
                playerListener?.onPlayerResume()
                durationTv.visibility = View.GONE
                positionTv.visibility = View.VISIBLE
            } else {
                playerListener?.onPlayerPause()
                durationTv.visibility = View.VISIBLE
                positionTv.visibility = View.GONE
            }
            Log.e("audioo", "onIsPlayingChanged")

        }

        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
        override fun onPlayerError(error: ExoPlaybackException) {
            println("$tag onPlayerError ${error.localizedMessage}")
            error.printStackTrace()
            Log.e("audioo", "onPlayerError  ${error.localizedMessage}")

        }

        override fun onPositionDiscontinuity(reason: Int) {}
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
        override fun onSeekProcessed() {}

    }

    init {
        R.color.exo_edit_mode_background_color
        val view =
            LayoutInflater.from(context).inflate(R.layout.exo_audio_player_layout, this, false)
        addView(view)
        playerControlView = findViewById(R.id.simple_exo)
        playerControlView.showTimeoutMs = -1
        durationTv = playerControlView.findViewById(R.id.exo_duration)
        positionTv = playerControlView.findViewById(R.id.exo_position)
        activity = context.activity()
        activity?.let {
            (it as LifecycleOwner).lifecycle.addObserver(this)
        }
        initializePlayer()
    }

    private tailrec fun Context?.activity(): Activity? = when (this) {
        is Activity -> this
        else -> (this as? ContextWrapper)?.baseContext?.activity()
    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(context).build()
        playerControlView.player = player
        playerControlView.setShowMultiWindowTimeBar(true)
        if (lastPosition != 0L) {
            player!!.seekTo(lastPosition)
        }
        initListener()
    }

    fun addAudios(sourceList: LinkedList<AudioModel>) {
        audioModels.clear()
        audioModels.addAll(sourceList)
        createDataSources()
    }

    fun setAudioPlayerEventListener(playerListener: AudioPlayerEventListener?) {
        this.playerListener = playerListener
    }

    fun seekTo(pos: Long) {
        player?.seekTo(pos)
    }


    private fun initListener() {
        player!!.addListener(playerEventListener)
    }

    private fun stringForTime(timeMs: Int): String {
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

    private fun createDataSources() {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, "joshskills")
        )
        val concatenatingMediaSource = ConcatenatingMediaSource()
        var lastTime: Long = 0

        for (audioModel in audioModels) {
            val factory = ProgressiveMediaSource.Factory(dataSourceFactory)
            if (audioModel.tag != null) {
                factory.setTag(audioModel.tag)
            }
            val audioSource: MediaSource = factory.createMediaSource(Uri.parse(audioModel.audioUrl))
            val clip = ClippingMediaSource(audioSource, audioModel.duration * 1000L)
            concatenatingMediaSource.addMediaSource(clip)
            lastTime += audioModel.duration * 1000L
            totalTime += audioModel.duration
        }
        player!!.prepare(concatenatingMediaSource)
        player!!.playWhenReady = false
        durationTv.text = stringForTime(totalTime.toInt())
        positionTv.text = stringForTime(totalTime.toInt())
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onStartPlayer() {
        if (player == null) {
            initializePlayer()
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResumePlayer() {
        resumePlayer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        pausePlayer()
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopPlayerEvent() {
    }

    private fun releasePlayer() {
        playerListener?.onPlayerReleased()
        if (player != null) {
            player!!.release()
            player = null
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        releasePlayer()
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
            durationTv.text = stringForTime(totalTime.toInt())
        }
    }

    private fun resumePlayer() {
        player?.run {
            //playWhenReady = true
            durationTv.text = stringForTime(currentPosition.toInt())
            seekTo(currentPosition)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isPlaying()) {
            pausePlayer()
        }
        Log.e("audioo", "onDetachedFromWindow")
    }
}