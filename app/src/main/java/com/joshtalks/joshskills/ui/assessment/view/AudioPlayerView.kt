package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import dm.audiostreamer.AudioStreamingManager
import dm.audiostreamer.CurrentSessionCallback
import dm.audiostreamer.MediaMetaData

class AudioPlayerView : FrameLayout, View.OnClickListener, CurrentSessionCallback {

    private lateinit var playButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var seekPlayerProgress: SeekBar
    private lateinit var timestamp: TextView
    private lateinit var progressWheel: ProgressBar
    private var streamingManager: AudioStreamingManager? = null
    private val audioMediaMetaData = MediaMetaData()

    private var id: String? = null
    private var url: String? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.audio_player_layout, this)
        playButton = findViewById(R.id.btnPlay)
        pauseButton = findViewById(R.id.btnPause)
        seekPlayerProgress = findViewById(R.id.seek_bar)
        progressWheel = findViewById(R.id.progress_bar)
        timestamp = findViewById(R.id.message_time)
        seekPlayerProgress.progress = 0
        streamingManager = AudioStreamingManager.getInstance(AppObjectController.joshApplication)
        streamingManager?.isPlayMultiple = false

        playButton.setOnClickListener(this)
        pauseButton.setOnClickListener(this)
        streamingManager?.subscribesCallBack(this)

        seekPlayerProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                    timestamp.text = Utils.formatDuration(progress)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    streamingManager?.onSeekTo(userSelectedPosition.toLong())
                }
            })

    }

    fun setupAudio(id: String, url: String) {
        this.id = id
        this.url = url
    }

    private fun configureAudio() {
        audioMediaMetaData.mediaId = id
        audioMediaMetaData.mediaUrl = url
        audioMediaMetaData.mediaDuration = 5000.toString()
        seekPlayerProgress.progress = 0
        seekPlayerProgress.max = 5000
        streamingManager?.setShowPlayerNotification(false)

    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnPlay) {
            playPause()
        } else if (v.id == R.id.btnPause) {
            playPause()
        }
    }

    private fun playPause() {
        streamingManager?.let {
            if (streamingManager?.currentAudio == null) {
                initAndPlay()
                return@let
            }

            if (streamingManager?.currentAudioId == id) {
                if (it.isPlaying) {
                    streamingManager?.handlePauseRequest()
                } else {
                    streamingManager?.handlePlayRequest()
                }
            } else {
                initAndPlay()
            }
        }
    }

    private fun initAndPlay() {
        configureAudio()
        streamingManager?.onPlay(audioMediaMetaData)
        progressWheel.visibility = View.VISIBLE
    }

    override fun currentSeekBarPosition(progress: Int) {
        seekPlayerProgress.progress = progress
    }

    override fun playSongComplete() {
        seekPlayerProgress.progress = 0
        pausingAudio()
    }

    override fun playNext(indexP: Int, currentAudio: MediaMetaData?) {

    }

    override fun updatePlaybackState(state: Int) {
        when (state) {
            PlaybackStateCompat.STATE_PLAYING -> {
                playingAudio()
                progressWheel.visibility = View.GONE
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                pausingAudio()
            }
            PlaybackStateCompat.STATE_NONE -> {
            }
            PlaybackStateCompat.STATE_STOPPED -> {
                seekPlayerProgress.progress = 0
                pausingAudio()
            }
            PlaybackStateCompat.STATE_BUFFERING -> {
                //  progressWheel.visibility = View.VISIBLE
            }
        }
    }

    override fun playCurrent(indexP: Int, currentAudio: MediaMetaData?) {
    }

    override fun playPrevious(indexP: Int, currentAudio: MediaMetaData?) {
    }

    private fun playingAudio() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE

    }

    private fun pausingAudio() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        seekPlayerProgress.progress = 0
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Log.e("onDetachedFromWindow", "AudioPlayer")
        streamingManager?.handlePauseRequest()
    }
}