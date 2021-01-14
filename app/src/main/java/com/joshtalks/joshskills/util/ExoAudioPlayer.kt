package com.joshtalks.joshskills.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.TAG
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener

class ExoAudioPlayer {
    private var progressTracker: ProgressTracker? = null
    private var progressUpdateListener: ProgressUpdateListener? = null
    var context: Context? = AppObjectController.joshApplication
    private val playerEventListener: Player.EventListener
    private var durationSet = false
    var currentPlayingUrl = EMPTY
    private var audioDuration: Long = 0


    init {
        playerEventListener = object : Player.EventListener {
            override fun onSeekProcessed() {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {
                    audioDuration = player?.duration ?: 0
                    durationSet = true
                }
                if (playbackState == ExoPlayer.STATE_ENDED)
                    playerListener?.complete()
                if (playbackState == ExoPlayer.STATE_READY)
                    progressUpdateListener?.onDurationUpdate(player?.duration)

                Log.d(
                    TAG,
                    "onPlayerStateChanged() called with: playWhenReady = $playWhenReady, playbackState = $playbackState, duration = ${player?.duration}"
                )
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

    private var player: SimpleExoPlayer? = null

    companion object {
        var LAST_ID: String = ""
        private var manager: ExoAudioPlayer? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): ExoAudioPlayer? {
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
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val extractorsFactory: DefaultExtractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
        player =
            SimpleExoPlayer.Builder(AppObjectController.joshApplication).setUseLazyPreparation(true)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context!!, extractorsFactory)
                )
                .build().apply {
                    setAudioAttributes(audioAttributes, true)
                }
        initListener()
    }

    fun seekTo(pos: Long) {
        Log.d(TAG, "seekTo() called with: pos = $pos")
        Log.d(
            TAG,
            "seekTo() called with: player = ${player?.currentPosition} duration = ${player?.duration}"
        )
        player?.seekTo(pos)
        Log.d(
            TAG,
            "seekTo() called with: player = ${player?.currentPosition} duration = ${player?.duration}"
        )
    }

    fun onPlay() {
        player?.playWhenReady = true
        playerListener?.onPlayerResume()
        progressTracker?.let { it.handler.post(it) }
    }

    fun onPause() {
        player?.playWhenReady = false
        playerListener?.onPlayerPause()
        progressTracker?.let { it.handler.removeCallbacks(it) }
    }


    private fun initListener() {
        player?.addListener(playerEventListener)
    }

    fun play(audioUrl: String, id: String = "", seekDuration: Long = 0) {
        currentPlayingUrl = audioUrl
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context!!,
            Util.getUserAgent(context!!, "joshskills")
        )

        LAST_ID = id

        val factory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val mediaItem: MediaItem = MediaItem.Builder()
            .setUri(Uri.parse(audioUrl))
            .setCustomCacheKey(
                Utils.getFileNameFromURL(audioUrl)
            )
            .build()

        with(player) {

        }
        player?.let {
            with(it) {
                val audioSource: MediaSource = factory.createMediaSource(mediaItem)
                setMediaSource(audioSource)
                repeatMode = ExoPlayer.REPEAT_MODE_OFF
                seekTo(seekDuration)
                playWhenReady = true
                progressTracker = ProgressTracker()
                setWakeMode(C.WAKE_MODE_NETWORK)
                setHandleAudioBecomingNoisy(true)
                prepare()

                Log.d(
                    TAG,
                    "play() called with: audioUrl = $audioUrl, id = $id, seekDuration = $seekDuration"
                )
            }
        }
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    fun getDuration() = audioDuration

    fun resumeOrPause() {
        player?.playWhenReady = player?.playWhenReady!!.not()
        if (isPlaying())
            progressTracker?.let { it.handler.post(it) }
        else
            progressTracker?.let { it.handler.removeCallbacks(it) }
    }

    inner class ProgressTracker : Runnable {
        internal val handler: Handler = Handler()
        override fun run() {
            val currentPosition = player?.currentPosition ?: 0
            progressUpdateListener?.onProgressUpdate(currentPosition)
            handler.postDelayed(this, 500 /* ms */)
        }

        init {
            handler.post(this)
        }
    }

    fun release() {
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