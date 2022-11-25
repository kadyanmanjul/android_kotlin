package com.joshtalks.joshskills.common.ui.lesson.reading

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.common.util.ExoAudioPlayer2
import com.muddzdev.styleabletoast.StyleableToast
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import timber.log.Timber

class ReadingPractiseAudioView : FrameLayout, LifecycleObserver,
    com.joshtalks.joshskills.common.util.ExoAudioPlayer2.ProgressUpdateListener, AudioPlayerEventListener {

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

    private lateinit var playPauseButton: MaterialPlayPauseButton
    private lateinit var seekbar: SeekBar
    private lateinit var timestamp: AppCompatTextView

    private var exoAudioManager: com.joshtalks.joshskills.common.util.ExoAudioPlayer2? = com.joshtalks.joshskills.common.util.ExoAudioPlayer2.getInstance()
    private val context = AppObjectController.joshApplication
    private var id: String = EMPTY
    private var url: String = EMPTY
    private var lastPosition: Long = 0L
    private var duration: Int = 0

    private fun init() {
        try {
            View.inflate(context, R.layout.reading_practise_audio_view, this)
            seekbar = findViewById(R.id.seek_bar)
            timestamp = findViewById(R.id.duration)
            playPauseButton = findViewById(R.id.btn_play_pause)
            seekbar.progress = 0
            playPauseButton.setOnClickListener {
                if (AppObjectController.isRecordingOngoing) {
                    return@setOnClickListener
                }
                if (playPauseButton.state == MaterialPlayPauseDrawable.State.Play) {
                    playPracticeAudio()
                    playAudio()
                } else {
                    playPauseButton.state = MaterialPlayPauseDrawable.State.Play
                    onPausePlayer()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun playPracticeAudio() {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(context.getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }
    }


    fun initAudioPlayer(url: String, duration: Int) {
        lastPosition = 0
        seekbar.progress = 0
        id = System.currentTimeMillis().toString()
        this.url = url
        this.duration = duration
        seekbar.max = duration
        timestamp.text = Utils.formatDuration(duration)
    }

    private fun removeSeekbarListener() {
        seekbar.setOnSeekBarChangeListener(null)
        exoAudioManager?.playerListener = null
        exoAudioManager?.setProgressUpdateListener(
            null
        )
    }

    private fun addListener() {
        seekbar.setOnSeekBarChangeListener(null)
        seekbar.setOnSeekBarChangeListener(
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
                    exoAudioManager?.seekTo(userSelectedPosition.toLong())
                }
            })
        exoAudioManager?.playerListener = this
        exoAudioManager?.setProgressUpdateListener(this)

    }

    private fun playAudio() {
        if (checkAudioIsDifferent()) {
            exoAudioManager?.let {
                removeSeekbarListener()
                addListener()
                if (com.joshtalks.joshskills.common.util.ExoAudioPlayer2.LAST_ID.isEmpty()) {
                    initAndPlay()
                    return@let
                }
                if (com.joshtalks.joshskills.common.util.ExoAudioPlayer2.LAST_ID == id) {
                    exoAudioManager?.resumeOrPause()
                    if (exoAudioManager?.isPlaying() == true) {
                        playPauseButton.state = MaterialPlayPauseDrawable.State.Pause
                    } else
                        playPauseButton.state = MaterialPlayPauseDrawable.State.Play
                } else {
                    initAndPlay()
                }
            }
        }
    }


    private fun checkAudioIsDifferent(): Boolean {
        if (com.joshtalks.joshskills.common.util.ExoAudioPlayer2.LAST_ID.isEmpty()) {
            return true
        }
        if (com.joshtalks.joshskills.common.util.ExoAudioPlayer2.LAST_ID == id) {
            return true
        }
        return true
    }

    private fun initAndPlay() {
        url.let {
            exoAudioManager?.play(it, id, lastPosition)
            seekbar.progress = lastPosition.toInt()
            playPauseButton.state = MaterialPlayPauseDrawable.State.Pause
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        exoAudioManager?.onPause()
        pausingAudioUI()
    }

    fun pausePlayer() {
        exoAudioManager?.run {
            if (isPlaying()) {
                this.onPause()
                pausingAudioUI()
            }
        }
    }

    private fun pausingAudioUI() {
        timestamp.text = Utils.formatDuration(duration)
        playPauseButton.state = MaterialPlayPauseDrawable.State.Play
        playPauseButton.jumpToState(MaterialPlayPauseDrawable.State.Play)
    }

    override fun complete() {
        playPauseButton.state = MaterialPlayPauseDrawable.State.Play
        playPauseButton.jumpToState(MaterialPlayPauseDrawable.State.Play)
        seekbar.progress = 0
        exoAudioManager?.seekTo(0)
        exoAudioManager?.onPause()
        exoAudioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        seekbar.progress = progress.toInt()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pausePlayer()
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        exoAudioManager?.release()
        Timber.tag("onDetachedFromWindow").e("AudioPlayer")
    }

}