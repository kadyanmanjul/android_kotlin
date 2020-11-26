package com.joshtalks.joshskills.ui.groupchat.uikit

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.util.ExoAudioPlayer
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Job
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit


class AudioV2PlayerView : FrameLayout, View.OnClickListener, LifecycleObserver,
    ExoAudioPlayer.ProgressUpdateListener, AudioPlayerEventListener {

    private lateinit var playButton: ImageView
    private lateinit var pauseButton: ImageView
    private lateinit var seekPlayerProgress: SeekBar
    private lateinit var timestamp: TextView
    private var audioManger: ExoAudioPlayer? = null
    private val context = AppObjectController.joshApplication

    private var id: String = EMPTY
    private var url: String? = null
    private var mediaDuration: Long? = null
    private var compositeDisposable = CompositeDisposable()
    private val jobs = arrayListOf<Job>()

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
        AppObjectController.createDefaultCacheDir()
        audioManger = ExoAudioPlayer.getInstance()
        ExoAudioPlayer.LAST_ID = EMPTY
        audioManger?.playerListener = this
        audioManger?.setProgressUpdateListener(this)

        View.inflate(context, R.layout.audio_player_layout2, this)
        playButton = findViewById(R.id.btnPlay)
        pauseButton = findViewById(R.id.btnPause)
        seekPlayerProgress = findViewById(R.id.seek_bar)
        timestamp = findViewById(R.id.message_time)
        seekPlayerProgress.progress = 0
        playButton.setOnClickListener(this)
        pauseButton.setOnClickListener(this)
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.VISIBLE

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
                    audioManger?.seekTo(userSelectedPosition.toLong())
                }
            })
    }

    fun bindView(
        id: Int,
        audioUrl: String,
        metadata: JSONObject? = null
    ) {
        this.id = id.toString()
        this.url = audioUrl
        try {
            this.mediaDuration = metadata?.getLong("audioDurationInMs")
            mediaDuration?.let {
                seekPlayerProgress.max = TimeUnit.MILLISECONDS.toSeconds(it).toInt()
                timestamp.text = Utils.formatDuration(it.toInt())

            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun setDefaultValue() {
        pausingAudio()
        seekPlayerProgress.progress = 0
    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnPlay) {
            playAudio()
        } else if (v.id == R.id.btnPause) {
            pausingAudio()
        }
    }

    private fun playAudio() {
        audioManger?.let {
            if (ExoAudioPlayer.LAST_ID.isEmpty()) {
                initAndPlay(url)
                return@let
            }
            if (ExoAudioPlayer.LAST_ID == id) {
                audioManger?.resumeOrPause()
                if (audioManger?.isPlaying() == true) {
                    playingAudio()
                } else
                    pausingAudio()
            } else {
                initAndPlay(url)
            }
        }
    }

    private fun initAndPlay(file: String?) {
        file?.let {
            audioManger?.play(it, id)
            seekPlayerProgress.progress = 0
            playingAudio()

        }
        //seekPlayerProgress.max = duration.toInt()
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
        setDefaultValue()
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
        setDefaultValue()
        audioManger?.release()
        ExoAudioPlayer.LAST_ID = ""
        Timber.tag("onDetachedFromWindow").e("AudioPlayer")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        audioManger?.onPause()
        pausingAudio()
    }

    override fun onProgressUpdate(progress: Long) {
        if (!JoshApplication.isAppVisible)
            onPausePlayer()
        seekPlayerProgress.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
    }

    override fun onPlayerPause() {
        pausingAudio()
    }

    override fun onPlayerResume() {
        playingAudio()
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
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        seekPlayerProgress.progress = 0
        audioManger?.seekTo(0)
        audioManger?.onPause()
    }
}

/*
        <RelativeLayout
            android:id="@+id/audio_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_centerVertical="true"
            android:layout_toEndOf="@+id/audio_view_sent">

            <FrameLayout
                android:id="@+id/fl_controller"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_alignParentStart="true"
                android:layout_centerHorizontal="true"
                android:layout_centerVertical="true"
                android:minWidth="@dimen/_36sdp">

                <androidx.appcompat.widget.AppCompatImageView
                    android:id="@+id/btnPlay"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:background="@null"
                    android:contentDescription="@string/play_button_description"
                    android:visibility="invisible"
                    app:srcCompat="@drawable/ic_play_24dp"
                    tools:visibility="visible" />

                <androidx.appcompat.widget.AppCompatImageView
                    android:id="@+id/btnPause"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:background="@null"
                    android:contentDescription="@string/play_button_description"
                    android:visibility="invisible"
                    app:srcCompat="@drawable/ic_pause_24dp"
                    tools:visibility="visible" />

            </FrameLayout>

            <FrameLayout
                android:id="@+id/fl_seek_bar"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_alignBaseline="@id/fl_controller"
                android:layout_centerHorizontal="true"
                android:layout_centerVertical="true"
                android:layout_marginTop="@dimen/_5sdp"
                android:layout_marginEnd="@dimen/_5sdp"
                android:layout_toEndOf="@id/fl_controller">

                <SeekBar
                    android:id="@+id/seekBar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:paddingStart="@dimen/_6sdp"
                    android:paddingEnd="@dimen/_6sdp"
                    android:progressTint="@color/colorPrimary"
                    android:thumb="@drawable/seek_thumb"
                    android:visibility="visible" />

            </FrameLayout>

            <androidx.appcompat.widget.AppCompatTextView
                android:id="@+id/txtCurrentDuration"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_below="@+id/fl_seek_bar"
                android:layout_alignStart="@id/fl_seek_bar"
                android:layout_marginTop="@dimen/_20sdp"
                android:textAlignment="center"
                android:textAppearance="@style/TextAppearance.JoshTypography.Caption_Normal_Regular"
                android:textColor="@color/gray_9E"
                app:layout_goneMarginTop="@dimen/spacing_large"
                tools:text="00:24" />

            <TextView
                android:id="@+id/message_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_below="@+id/fl_seek_bar"
                android:layout_alignEnd="@id/fl_seek_bar"
                android:layout_marginTop="@dimen/_20sdp"
                android:layout_marginEnd="@dimen/_5sdp"
                android:gravity="center|end"
                android:textAlignment="center"
                android:textAppearance="@style/TextAppearance.JoshTypography.Caption_Normal_Regular"
                android:textColor="@color/gray_9E"
                app:layout_goneMarginTop="@dimen/spacing_large"
                tools:ignore="MissingPrefix"
                tools:text="12:35PM" />
        </RelativeLayout>

* */