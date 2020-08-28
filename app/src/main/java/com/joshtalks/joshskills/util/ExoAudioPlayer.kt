package com.joshtalks.joshskills.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener

class ExoAudioPlayer {


    private var progressTracker: ProgressTracker? = null
    private var progressUpdateListener: ProgressUpdateListener? = null
    var context: Context?
    private val playerEventListener: Player.EventListener

    init {
        println("ExoAudioPlayer.init block")
        context = JoshApplication.instance?.applicationContext
        playerEventListener = object : Player.EventListener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
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

    private var player: SimpleExoPlayer? = null

    companion object {
        var LAST_ID: String = ""
        private var manager: ExoAudioPlayer? = null

        fun getInstance(): ExoAudioPlayer? {
            if (manager == null) {
                manager = ExoAudioPlayer()
            }
            return manager
        }
    }

    fun setProgressUpdateListener(progressUpdateListener: ProgressUpdateListener?) {
        this.progressUpdateListener = progressUpdateListener
        progressTracker =
            ProgressTracker(player!!)
    }

    private fun initializePlayer() {
        context?.let { player = SimpleExoPlayer.Builder(it).build() }
        initListener()
    }

    fun seekTo(pos: Long) {
        player?.seekTo(pos)
    }

    fun onPlay() {
        player?.playWhenReady = true
        playerListener?.onPlayerResume()
    }

    fun onPause() {
        player?.playWhenReady = false
        playerListener?.onPlayerPause()
//        player?.seekTo(0, 0)
    }


    private fun initListener() {
        player?.addListener(playerEventListener)
    }

    fun play(audioUrl: String, id: String = "") {
        println("audioUrl = [${audioUrl}]")
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context!!, "joshskills")
        )

        LAST_ID = id
        val factory = ProgressiveMediaSource.Factory(dataSourceFactory)
        val audioSource: MediaSource =
            factory.createMediaSource(Uri.parse(audioUrl))
        player!!.prepare(audioSource)
        player!!.playWhenReady = true
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }

    private fun pausePlayer() {
        player?.run {
            playWhenReady = false
        }
    }

    private fun resumePlayer() {
        player?.run {
            seekTo(currentPosition)
            playWhenReady = true
        }
    }

    fun resumeOrPause() {
        player?.playWhenReady = player?.playWhenReady!!.not()
    }

    inner class ProgressTracker(private val player: SimpleExoPlayer) : Runnable {
        internal val handler: Handler = Handler()
        override fun run() {
            val currentPosition = player.currentPosition
            progressUpdateListener?.onProgressUpdate(currentPosition)
            handler.postDelayed(this, 500 /* ms */)
        }

        init {
            handler.post(this)
        }
    }

    fun release() {
//        player?.stop(true)
//        player?.release()

        player?.playWhenReady = false
        if (progressTracker != null) {
            progressTracker!!.handler.removeCallbacks(progressTracker)
        }
        if (playerListener != null)
            playerListener = null
    }

    interface ProgressUpdateListener {
        fun onProgressUpdate(progress: Long)
        fun onDurationUpdate(duration: Long?)
    }
}