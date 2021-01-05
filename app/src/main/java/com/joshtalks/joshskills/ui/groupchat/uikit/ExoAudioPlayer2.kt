package com.joshtalks.joshskills.ui.groupchat.uikit

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.videoplayer.PlaybackSpeed

class ExoAudioPlayer2 {
    private var progressTracker: ProgressTracker? = null
    private var progressUpdateListener: ProgressUpdateListener? = null
    var context: Context? = AppObjectController.joshApplication
    private val playerEventListener: Player.EventListener
    private var durationSet = false
    var currentPlayingUrl = EMPTY
    private var audioDuration: Long = 0

    private val player: SimpleExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        SimpleExoPlayer.Builder(AppObjectController.joshApplication).setUseLazyPreparation(true)
            .build().apply {
                setAudioAttributes(audioAttributes, true)
            }
    }

    init {
        playerEventListener = object : Player.EventListener {
            override fun onSeekProcessed() {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                    audioDuration = player.duration
                    durationSet = true
                }
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    playerListener?.complete()
                    return
                }
                if (playbackState == ExoPlayer.STATE_READY && playWhenReady.not()) {
                    progressUpdateListener?.onDurationUpdate(player.duration)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying)
                    playerListener?.onPlayerResume()
                else
                    playerListener?.onPlayerPause()
            }

        }

        initializePlayer()
    }

    var playerListener: AudioPlayerEventListener? = null

    companion object {
        var LAST_ID: String = ""
        private var manager: ExoAudioPlayer2? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): ExoAudioPlayer2? {
            if (manager == null) {
                manager = ExoAudioPlayer2()
            }
            return manager
        }
    }

    fun setProgressUpdateListener(progressUpdateListener: ProgressUpdateListener?) {
        this.progressUpdateListener = progressUpdateListener
    }

    private fun initializePlayer() {
        initListener()
    }

    fun seekTo(pos: Long) {
        player.seekTo(pos)
    }

    fun onPlay() {
        player.playWhenReady = true
        playerListener?.onPlayerResume()
        progressTracker?.let { it.handler.post(it) }
    }

    fun onPause() {
        player.playWhenReady = false
        playerListener?.onPlayerPause()
        progressTracker?.let { it.handler.removeCallbacks(it) }
    }

    fun onPauseComplete() {
        player.seekTo(0)
        player.playWhenReady = false
        player.playbackState
        progressTracker?.let {
            it.handler.removeCallbacks(it)
        }
    }


    private fun initListener() {
        player.addListener(playerEventListener)
    }


    fun onChangePlaybackSpeed(playbackSpeed: PlaybackSpeed) {

    }

    fun play(
        audioUrl: String,
        id: String = "",
        seekDuration: Long = 0,
        isPlaybackSpeed: Boolean = false
    ) {
        var param = PlaybackParameters(1F)
        if (isPlaybackSpeed) {
            param = PlaybackParameters(0.50F, 1F)//pitch sexy hai
        }
        player.setPlaybackParameters(param)
        currentPlayingUrl = audioUrl
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context!!,
            Util.getUserAgent(context!!, "joshskills")
        )

        LAST_ID = id
        val factory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(Uri.parse(audioUrl))
            .build()
        val audioSource: MediaSource = factory.createMediaSource(mediaItem)
        player.setMediaSource(audioSource)
        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player.seekTo(seekDuration)
        player.playWhenReady = true
        progressTracker =
            ProgressTracker(player)
        player.prepare()

    }

    fun playClipAudio(
        audioUrl: String,
        id: String = "",
        startDuration: Long = 0,
        endDuration: Long = 0
    ) {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context!!, Util.getUserAgent(context!!, "joshskills")
        )
        LAST_ID = id
        val factory = ProgressiveMediaSource.Factory(dataSourceFactory)

        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(Uri.parse(audioUrl))
            .setClipStartPositionMs(1000)
            .setClipEndPositionMs(3000)
            .build()
        val audioSource: MediaSource =
            factory.createMediaSource(mediaItem)

        val concatenatingMediaSource = ConcatenatingMediaSource()

        val clip = ClippingMediaSource(audioSource, 1000L, 3000L)
        concatenatingMediaSource.addMediaSource(clip)
        player.setMediaSource(concatenatingMediaSource)
        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player.playWhenReady = true
        progressTracker = ProgressTracker(player)
        player.prepare()
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun getDuration() = audioDuration

    fun resumeOrPause() {
        player.playWhenReady = player.playWhenReady.not()
        if (isPlaying())
            progressTracker?.let { it.handler.post(it) }
        else
            progressTracker?.let { it.handler.removeCallbacks(it) }
    }

    inner class ProgressTracker(private val player: SimpleExoPlayer) : Runnable {
        internal val handler: Handler = Handler()
        override fun run() {
            val currentPosition = player.currentPosition
            progressUpdateListener?.onProgressUpdate(currentPosition)
            handler.postDelayed(this, 50 /* ms */)
        }

        init {
            handler.post(this)
        }
    }

    fun release() {
        player.playWhenReady = false
        progressTracker?.let { it.handler.removeCallbacks(it) }
        currentPlayingUrl = EMPTY
        if (playerListener != null)
            playerListener = null
    }

    interface ProgressUpdateListener {
        fun onProgressUpdate(progress: Long) {}
        fun onDurationUpdate(duration: Long?) {}
    }
}