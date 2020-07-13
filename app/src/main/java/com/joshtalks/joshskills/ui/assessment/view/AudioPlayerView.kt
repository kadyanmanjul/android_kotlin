package com.joshtalks.joshskills.ui.assessment.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import dm.audiostreamer.AudioStreamingManager
import dm.audiostreamer.CurrentSessionCallback
import dm.audiostreamer.MediaMetaData

class AudioPlayerView : FrameLayout, View.OnClickListener, CurrentSessionCallback {


    private var playButton: ImageView? = null
    private var pauseButton: ImageView? = null
    private var seekPlayerProgress: SeekBar? = null
    private var timestamp: TextView? = null
    private var progressWheel: ProgressBar? = null
    private var streamingManager: AudioStreamingManager? = null
    private val audioMediaMetaData = MediaMetaData()

    private var id: Int? = null
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
        seekPlayerProgress = findViewById(R.id.seek)
        progressWheel = findViewById(R.id.progress_bar)
        timestamp = findViewById(R.id.message_time)
        seekPlayerProgress?.progress = 0
        streamingManager = AudioStreamingManager.getInstance(AppObjectController.joshApplication)
        streamingManager?.isPlayMultiple = false

        playButton?.setOnClickListener(this)
        pauseButton?.setOnClickListener(this)
        streamingManager?.subscribesCallBack(this)

    }

    fun setupAudio(id: Int, url: String) {
        this.id = id
        this.url = url
        configureAudio()
    }

    private fun configureAudio() {
        audioMediaMetaData.mediaId = id?.toString()
        audioMediaMetaData.mediaUrl = url
        audioMediaMetaData.mediaDuration = 5000.toString()
        seekPlayerProgress?.progress = 0
        seekPlayerProgress?.max = 5000
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
            if (it.isPlaying) {
                streamingManager?.handlePauseRequest()
            } else {
                streamingManager?.handlePlayRequest()
            }
        }
    }

    override fun currentSeekBarPosition(progress: Int) {

    }

    override fun playSongComplete() {
    }

    override fun playNext(indexP: Int, currentAudio: MediaMetaData?) {
    }

    override fun updatePlaybackState(state: Int) {
    }

    override fun playCurrent(indexP: Int, currentAudio: MediaMetaData?) {
    }

    override fun playPrevious(indexP: Int, currentAudio: MediaMetaData?) {
    }


}