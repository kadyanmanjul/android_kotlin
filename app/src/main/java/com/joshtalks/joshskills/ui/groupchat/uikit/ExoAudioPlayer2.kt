package com.joshtalks.joshskills.ui.groupchat.uikit

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener

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
                if (!playWhenReady) {
                    playerListener?.onPlayerPause()
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
        if (progressUpdateListener != null)
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

    fun play(
        audioUrl: String,
        id: String = "",
        seekDuration: Long = 0,
        isPlaybackSpeed: Boolean = false,
        delayProgress: Long = 50,
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
        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player.seekTo(seekDuration)
        player.playWhenReady = true
        progressTracker = ProgressTracker(player, delayProgress)
        player.setWakeMode(C.WAKE_MODE_NETWORK)
        player.setHandleAudioBecomingNoisy(true)
        val audioSource: MediaSource =
            factory.createMediaSource(Uri.parse(audioUrl))
        player.prepare(audioSource)
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun getDuration() = audioDuration

    fun resumeOrPause() {
        player.playWhenReady = player.playWhenReady.not()
        if (isPlaying()) {
            progressTracker?.let { it.handler.post(it) }
        } else {
            progressTracker?.let { it.handler.removeCallbacks(it) }
        }
    }

    inner class ProgressTracker(
        private val player: SimpleExoPlayer,
        private val delayMillis: Long = 50
    ) : Runnable {
        internal val handler: Handler = Handler()
        override fun run() {
            val currentPosition = player.currentPosition
            progressUpdateListener?.onProgressUpdate(currentPosition)
            handler.postDelayed(this, delayMillis /* ms */)
        }

        init {
            handler.post(this)
        }
    }

    fun release() {
        player.playWhenReady = false
        progressTracker?.let { it.handler.removeCallbacks(it) }
        currentPlayingUrl = EMPTY
    }

    interface ProgressUpdateListener {
        fun onProgressUpdate(progress: Long) {}
        fun onDurationUpdate(duration: Long?) {}
    }
}
