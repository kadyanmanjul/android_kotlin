package com.joshtalks.joshskills.ui.groupchat.uikit

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayerEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PauseAudioEventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import timber.log.Timber


class AudioV2PlayerView : FrameLayout, View.OnClickListener, LifecycleObserver,
    ExoAudioPlayer2.ProgressUpdateListener, AudioPlayerEventListener {

    private var exoAudioManager: ExoAudioPlayer2? = ExoAudioPlayer2.getInstance()
    private val context = AppObjectController.joshApplication
    private var id: String = EMPTY
    private var url: String? = null
    private lateinit var playButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var seekPlayerProgress: SeekBar
    private lateinit var timestamp: TextView
    private lateinit var progressBar: ProgressBar
    private val compositeDisposable = CompositeDisposable()
    private var lastPosition: Long = 0L
    private var mediaDuration: Long = 0


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
        View.inflate(context, R.layout.audio_player_layout2, this)
        playButton = findViewById(R.id.btnPlay)
        pauseButton = findViewById(R.id.btnPause)
        seekPlayerProgress = findViewById(R.id.seek_bar)
        timestamp = findViewById(R.id.message_time)
        progressBar = findViewById(R.id.progress_bar)
        seekPlayerProgress.progress = 0
        playButton.setOnClickListener(this)
        pauseButton.setOnClickListener(this)
        playButton.visibility = View.VISIBLE
        // pauseButton.visibility = View.VISIBLE

        RxBus2.listenWithoutDelay(PauseAudioEventBus::class.java)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                onPausePlayer()
            }
    }

    fun bindView(
        id: Int,
        audioUrl: String,
        metadata: JSONObject? = null
    ) {
        this.id = id.toString()
        this.url = audioUrl
        try {
            if (metadata != null && metadata.has("audioDurationInMs")) {
                this.mediaDuration = metadata.getLong("audioDurationInMs")
            }
            mediaDuration.let {
                seekPlayerProgress.max = it.toInt()
                timestamp.text = Utils.formatDuration(it.toInt())
            }

            if (ExoAudioPlayer2.LAST_ID.isBlank().not() && ExoAudioPlayer2.LAST_ID == this.id) {
                addListener()
                setView()
            } else {
                removeSeekbarListener()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        Timber.tag("AudioV2PlayerView").e("" + mediaDuration)
    }

    fun showProgressBarVisible(isVisible: Boolean) {
        if (isVisible) {
            progressBar.visibility = VISIBLE
        } else {
            progressBar.visibility = GONE
        }
    }

    private fun setDefaultValue() {
        pausingAudio()
        seekPlayerProgress.progress = lastPosition.toInt()
        seekPlayerProgress.max = mediaDuration.toInt()
    }

    fun setThemeColor(colorId: Int) {
        playButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorId))
        progressBar.progressTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorId))
        pauseButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorId))
        seekPlayerProgress.thumbTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorId))
        seekPlayerProgress.progressTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorId))
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnPlay) {
            playAudio()
        } else if (v.id == R.id.btnPause) {
            pausingAudio()
            exoAudioManager?.resumeOrPause()
        }
    }

    private fun checkAudioIsDifferent(): Boolean {
        if (ExoAudioPlayer2.LAST_ID.isEmpty()) {
            return true
        }
        if (ExoAudioPlayer2.LAST_ID == id) {
            return true
        }
        removeSeekbarListener()
        return true
    }

    private fun removeSeekbarListener() {
        seekPlayerProgress.setOnSeekBarChangeListener(null)
        exoAudioManager?.playerListener = null
        exoAudioManager?.setProgressUpdateListener(
            null
        )
    }

    private fun addListener() {
        seekPlayerProgress.setOnSeekBarChangeListener(null)
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
                if (ExoAudioPlayer2.LAST_ID.isEmpty()) {
                    initAndPlay(url)
                    return@let
                }
                if (ExoAudioPlayer2.LAST_ID == id) {
                    exoAudioManager?.resumeOrPause()
                    if (exoAudioManager?.isPlaying() == true) {
                        playingAudio()
                    } else
                        pausingAudio()
                } else {
                    initAndPlay(url)
                }
            }
        }
    }


    private fun initAndPlay(file: String?) {
        file?.let {
            exoAudioManager?.play(it, id, lastPosition)
            seekPlayerProgress.progress = lastPosition.toInt()
            playingAudio()
        }
    }

    private fun playingAudio() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
    }

    private fun pausingAudio() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        timestamp.text = Utils.formatDuration(mediaDuration.toInt())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        exoAudioManager?.onPause()
        pausingAudio()
    }

    override fun onProgressUpdate(progress: Long) {
        if (JoshApplication.isAppVisible.not()) {
            onPausePlayer()
        }
        if (seekPlayerProgress.progress >= mediaDuration) {
            lastPosition = 0
            seekPlayerProgress.progress = 0
            complete()
            return
        }
        seekPlayerProgress.progress = progress.toInt()
        lastPosition = progress
    }


    override fun onDurationUpdate(duration: Long?) {
        /*  duration?.toInt()?.let {
              seekPlayerProgress.max = it
          }*/
    }


    override fun onPlayerPause() {
        Timber.tag("AudioV2PlayerView").e("onPlayerPause")
    }

    override fun onPlayerResume() {
        Timber.tag("AudioV2PlayerView").e("onPlayerResume")
        RxBus2.publish(AudioPlayerEventBus(PlaybackInfoListener.State.PLAYING, id))
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onPlayerReleased() {
        Timber.tag("AudioV2PlayerView").e("onPlayerReleased")
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        Timber.tag("AudioV2PlayerView").e("complete")
        exoAudioManager?.onPauseComplete()
        exoAudioManager?.seekTo(0)
        seekPlayerProgress.progress = 0
        setDefaultValue()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(AudioPlayerEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.id != id) {
                        pausingAudio()
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setDefaultValue()
        subscribeRXBus()
        if (ExoAudioPlayer2.LAST_ID.isBlank().not() && ExoAudioPlayer2.LAST_ID == this.id) {
            addListener()
            setView()
        } else {
            removeSeekbarListener()
        }
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeSeekbarListener()
        //setDefaultValue()
        //exoAudioManager?.release()
        //compositeDisposable.clear()
        Timber.tag("onDetachedFromWindow").e("AudioPlayer")
    }

    fun setView() {
        exoAudioManager?.let {
            if (it.isPlaying()) {
                playingAudio()
            } else {
                pausingAudio()
            }
        }
    }
}
