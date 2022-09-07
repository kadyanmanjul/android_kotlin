package com.joshtalks.joshskills.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener

class ExoAudioPlayer {
    private val TAG = "ExoAudioPlayer"
    private var progressTracker: ProgressTracker? = null
    private var progressUpdateListener: ProgressUpdateListener? = null
    var context: Context? = AppObjectController.joshApplication
    private val playerEventListener: Player.Listener
    private var durationSet = false
    var currentPlayingUrl = EMPTY
    private var audioDuration: Long = 0


    init {
        playerEventListener = object : Player.Listener {

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                    audioDuration = player?.duration ?: 0
                    durationSet = true
                }
                if (playbackState == ExoPlayer.STATE_ENDED)
                    playerListener?.complete()
                if (playbackState == ExoPlayer.STATE_READY)
                    progressUpdateListener?.onDurationUpdate(player?.duration)
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

    private var player: ExoPlayer? = null

    companion object {
        var LAST_ID: String = ""
        private var manager: ExoAudioPlayer? = null

        @JvmStatic
        @Synchronized
        fun getInstance(lifecycle: Lifecycle? = null): ExoAudioPlayer? {
            if (manager == null) {
                manager = ExoAudioPlayer()
            }
            return manager
        }
    }

    fun setProgressUpdateListener(progressUpdateListener: ProgressUpdateListener?) {
        this.progressUpdateListener = progressUpdateListener
    }

    private fun initializePlayer() {
        Log.d(TAG, "initializePlayer: ")
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val extractorsFactory: DefaultExtractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        player =
            ExoPlayer.Builder(AppObjectController.joshApplication).setUseLazyPreparation(true)
                .build().apply {
                    setAudioAttributes(audioAttributes, true)
                }
        initListener()
    }

    fun seekTo(pos: Long) {
        player?.seekTo(pos)
    }

    fun onPlay() {
        player?.playWhenReady = true
        playerListener?.onPlayerResume()
        progressTracker?.let { it.handler.post(it) }
    }

    fun onPause() {
        Log.d(TAG, "onPause: Audio Manager ${player?.playWhenReady}")
        player?.playWhenReady = false
        Log.d(TAG, "onPause: Audio Manager ${player?.playWhenReady}")
        playerListener?.onPlayerPause()
        progressTracker?.let { it.handler.removeCallbacks(it) }
    }

    private fun initListener() {
        Log.d(TAG, "initListener: ")
        player?.addListener(playerEventListener)
    }

    fun play(audioUrl: String, id: String = "", seekDuration: Long = 0) {
        Log.d(TAG, "play: ")
        currentPlayingUrl = audioUrl
        val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context!!)
        LAST_ID = id
        val factory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val audioSource: MediaSource =
            factory.createMediaSource(MediaItem.fromUri(Uri.parse(audioUrl)))
        player?.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        player?.playWhenReady = true
        progressTracker = ProgressTracker()
        player?.setMediaSource(audioSource)
        player?.prepare()
        player?.seekTo(seekDuration)
    }

    fun isPlaying(): Boolean {
        Log.d(TAG, "isPlaying: ")
        return player?.isPlaying ?: false
    }

    fun getDuration() = audioDuration

    fun resumeOrPause() {
        Log.d(TAG, "resumeOrPause: ")
        player?.playWhenReady = player?.playWhenReady!!.not()
        if (isPlaying())
            progressTracker?.let { it.handler.post(it) }
        else
            progressTracker?.let { it.handler.removeCallbacks(it) }
    }

    inner class ProgressTracker : Runnable {
        internal val handler: Handler = Handler(Looper.getMainLooper())
        override fun run() {
            val currentPosition = player?.currentPosition ?: 0
            progressUpdateListener?.onProgressUpdate(currentPosition)
            handler.postDelayed(this, 50 /* ms */)
        }

        init {
            handler.post(this)
        }
    }

    fun release() {
        Log.d(TAG, "release: Audio Manager")
        player?.playWhenReady = false
        progressTracker?.let { it.handler.removeCallbacks(it) }
        currentPlayingUrl = EMPTY
        if (playerListener != null)
            playerListener = null
    }

    interface ProgressUpdateListener {
        fun onProgressUpdate(progress: Long)
        fun onDurationUpdate(duration: Long?)
    }
}