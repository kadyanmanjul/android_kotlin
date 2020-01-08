package com.joshtalks.joshskills.core.custom_ui.audioplayer.service

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.JcStatus
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.Origin
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.AudioAssetsInvalidException
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.AudioFilePathInvalidException
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.AudioRawInvalidException
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.AudioUrlInvalidException
import com.joshtalks.joshskills.core.custom_ui.audioplayer.model.JcAudio
import java.io.File
import java.io.IOException


const val THREAD_TIME: Long = 50

class JcPlayerService : Service(), Player.EventListener {

    private val TAG = JcPlayerService::class.java.simpleName
    private val binder = JcPlayerServiceBinder()
    private var mediaPlayer: SimpleExoPlayer? = null
    var isPlaying: Boolean = false
        private set

    var isPaused: Boolean = true
        private set

    var currentAudio: JcAudio? = null
        private set

    private var totalDuration: Long = 0L
    private val jcStatus = JcStatus()


    private var assetFileDescriptor: AssetFileDescriptor? = null // For Asset and Raw file.

    var serviceListener: JcPlayerServiceListener? = null


    inner class JcPlayerServiceBinder : Binder() {
        val service: JcPlayerService
            get() = this@JcPlayerService
    }

    override fun onBind(intent: Intent): IBinder? = binder


    fun play(jcAudio: JcAudio): JcStatus {
        val tempJcAudio = currentAudio
        currentAudio = jcAudio
        var status = JcStatus()

        if (isAudioFileValid(jcAudio.path, jcAudio.origin)) {
            try {
                mediaPlayer?.let {
                    if (isPlaying) {
                        stop()
                        play(jcAudio)
                    } else {
                        if (tempJcAudio !== jcAudio) {
                            stop()
                            play(jcAudio)
                        } else {
                            status = updateStatus(jcAudio, JcStatus.PlayState.CONTINUE)
                            updateTime()
                            serviceListener?.onContinueListener(status)
                        }
                    }
                } ?: let {
                    val rendererFactory = DefaultRenderersFactory(this).setExtensionRendererMode(
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    )
                    val trackSelector = DefaultTrackSelector()
                    mediaPlayer =
                        ExoPlayerFactory.newSimpleInstance(this, rendererFactory, trackSelector)
                    mediaPlayer?.addListener(this)
                    mediaPlayer?.also {
                        it.playWhenReady = true
                        val mediaSource: MediaSource = createMediaSource(jcAudio.path)
                        it.prepare(mediaSource, true, false)

                        status = updateStatus(jcAudio, JcStatus.PlayState.PREPARING)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            throwError(jcAudio.path, jcAudio.origin)
        }

        return status
    }

    private fun createMediaSource(uri: String): MediaSource {
        val extractorFactory = DefaultExtractorsFactory()
        val dataSource = DefaultDataSourceFactory(this, "GenericUserAgent", null)
        return ProgressiveMediaSource.Factory(dataSource, extractorFactory)
            .createMediaSource(Uri.parse(uri))
    }

    fun pause(jcAudio: JcAudio): JcStatus {
        val status = updateStatus(jcAudio, JcStatus.PlayState.PAUSE)
        serviceListener?.onPausedListener(status)
        return status
    }

    fun stop(): JcStatus {
        val status = updateStatus(status = JcStatus.PlayState.STOP)
        serviceListener?.onStoppedListener(status)
        return status
    }


    fun seekTo(time: Int) {
        mediaPlayer?.seekTo(time.toLong())
    }


    private fun updateStatus(jcAudio: JcAudio? = null, status: JcStatus.PlayState): JcStatus {
        currentAudio = jcAudio
        jcStatus.jcAudio = jcAudio
        jcStatus.playState = status
        jcStatus.duration = totalDuration

        try {
            Handler(Looper.getMainLooper()).post {
                mediaPlayer?.let {
                    //    jcStatus.duration = it.duration
                    jcStatus.currentPosition = it.currentPosition

                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()

        }

        when (status) {
            JcStatus.PlayState.PLAY -> {
                try {
                    mediaPlayer?.playWhenReady
                    isPlaying = true
                    isPaused = false

                } catch (exception: Exception) {
                    serviceListener?.onError(exception)
                }
            }

            JcStatus.PlayState.STOP -> {
                mediaPlayer?.let {
                    it.stop()
                    it.release()
                    mediaPlayer = null
                }

                isPlaying = false
                isPaused = true
            }

            JcStatus.PlayState.PAUSE -> {
                mediaPlayer?.playWhenReady = false
                mediaPlayer?.playbackState
                isPlaying = false
                isPaused = true
            }

            JcStatus.PlayState.PREPARING -> {
                isPlaying = false
                isPaused = true
            }

            JcStatus.PlayState.PLAYING -> {
                isPlaying = true
                isPaused = false
            }
            JcStatus.PlayState.CONTINUE -> {
                isPlaying = true
                isPaused = false
                mediaPlayer?.playWhenReady = true
                mediaPlayer?.playbackState
                mediaPlayer?.currentPosition?.let { mediaPlayer?.seekTo(it) }
            }

            else -> { // CONTINUE case
                jcAudio?.let { play(it) }
                isPlaying = true
                isPaused = false
            }
        }

        return jcStatus
    }

    private fun updateTime() {
        object : Thread() {
            override fun run() {
                while (isPlaying) {
                    try {
                        val status = updateStatus(currentAudio, JcStatus.PlayState.PLAYING)
                        serviceListener?.onTimeChangedListener(status)
                        sleep(THREAD_TIME)
                        //     sleep(TimeUnit.SECONDS.toMillis(1))
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun isAudioFileValid(path: String, origin: Origin): Boolean {
        when (origin) {
            Origin.URL -> return path.startsWith("http") || path.startsWith("https")

            Origin.RAW -> {
                assetFileDescriptor = null
                assetFileDescriptor =
                    applicationContext.resources.openRawResourceFd(Integer.parseInt(path))
                return assetFileDescriptor != null
            }

            Origin.ASSETS -> return try {
                assetFileDescriptor = null
                assetFileDescriptor = applicationContext.assets.openFd(path)
                assetFileDescriptor != null
            } catch (e: IOException) {
                e.printStackTrace() //TODO: need to give user more readable error.
                false
            }

            Origin.FILE_PATH -> {
                val file = File(path)
                //TODO: find an alternative to checking if file is exist, this code is slower on average.
                //read more: http://stackoverflow.com/a/8868140
                return file.exists()
            }

            else -> // We should never arrive here.
                return false // We don't know what the origin of the Audio File
        }
    }

    private fun throwError(path: String, origin: Origin) {
        when (origin) {
            Origin.URL -> throw AudioUrlInvalidException(path)

            Origin.RAW -> try {
                throw AudioRawInvalidException(path)
            } catch (e: AudioRawInvalidException) {
                e.printStackTrace()
            }

            Origin.ASSETS -> try {
                throw AudioAssetsInvalidException(path)
            } catch (e: AudioAssetsInvalidException) {
                e.printStackTrace()
            }

            Origin.FILE_PATH -> try {
                throw AudioFilePathInvalidException(path)
            } catch (e: AudioFilePathInvalidException) {
                e.printStackTrace()
            }
        }
    }

    fun getMediaPlayer(): SimpleExoPlayer? {
        return mediaPlayer
    }

    fun finalize() {
        onDestroy()
        stopSelf()
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        super.onPlaybackParametersChanged(playbackParameters)
        // Log.e("TAG", "onPlaybackParametersChanged")
    }

    override fun onSeekProcessed() {
        super.onSeekProcessed()
        //  Log.e("TAG", "onSeekProcessed")

    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {
        super.onTracksChanged(trackGroups, trackSelections)
        // Log.e("TAG", "onTracksChanged")

    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        super.onPlayerError(error)
        //  Log.e("TAG", "onPlayerError")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        super.onLoadingChanged(isLoading)
        // Log.e("TAG", "onLoadingChanged")

    }

    override fun onPositionDiscontinuity(reason: Int) {
        super.onPositionDiscontinuity(reason)
        //  Log.e("TAG", "onPositionDiscontinuity")


    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        //   Log.e("TAG", "onRepeatModeChanged")

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        //  Log.e("TAG", "onShuffleModeEnabledChanged")

    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
        super.onTimelineChanged(timeline, manifest, reason)
        //  Log.e("TAG", "onTimelineChanged")

    }


    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        updateProgressBar()
        when (playbackState) {
            Player.STATE_READY -> {
                // Log.e(TAG, "onPrepared() " + mediaPlayer!!.bufferedPercentage + "% buffered")
                synchronized(this) {
                    if (mediaPlayer == null) return
                    if (playWhenReady) {
                        isPlaying = true
                        isPaused = false
                        if (mediaPlayer?.duration != null) {
                            totalDuration = mediaPlayer?.duration!!
                        }

                        onPrepared(getMediaPlayer()!!)

                    } else {
                        isPaused = true
                        isPlaying = false

                    }

                    if (isPlaying) {
                        //    Log.d(TAG, "Already started. Ignoring.")
                        return
                    }

                }

                //notifyOnStart()
                //progressEventHandler.sendEmptyMessage(0)
            }

            Player.STATE_ENDED -> {
                //   Log.e(TAG, "onComplete")
                synchronized(this) {
                    mediaPlayer = null
                    serviceListener?.onCompletedListener()
                    isPlaying = false
                    isPaused = true

/*
                    sensorManager.unregisterListener(this@AudioSlidePlayer)

                    if (wakeLock != null && wakeLock!!.isHeld()) {
                        if (Build.VERSION.SDK_INT >= 21) {
                            wakeLock!!.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                        }
                    }*/
                }

                //notifyOnStop()
                // progressEventHandler.removeMessages(0)
            }
        }
    }

    private fun updateProgressBar() {
        if (mediaPlayer?.duration != null) {
            totalDuration = mediaPlayer?.duration!!
        }
        // handler.postDelayed(updateProgressAction, delayMs);
        /* val position = mediaPlayer?.contentPosition
         var bufferedPosition = mediaPlayer?.bufferedPosition;
         // Schedule an update if necessary.
         val playbackState = mediaPlayer?.playbackState ?: Player.STATE_IDLE
         if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
             var delayMs = 0L
             if (mediaPlayer?.playWhenReady!! && playbackState == Player.STATE_READY) {
                 delayMs = 1000 - (position!! % 1000);
                 if (delayMs < 200) {
                     delayMs += 1000;
                 }
             } else {
                 delayMs = 1000;
             }
             //   handler.postDelayed(updateProgressAction, delayMs);
         }*/
    }

    fun onPrepared(mediaPlayer: SimpleExoPlayer?) {
        updateTime()
        mediaPlayer?.let {
            this.mediaPlayer = it
        }
        val status = updateStatus(currentAudio, JcStatus.PlayState.PLAY)
        serviceListener?.onPreparedListener(status)
    }


}
