package com.joshtalks.joshskills.ui.chat.vh

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import kotlin.random.Random


class GrammarHeadingView : FrameLayout, LifecycleObserver, AudioPlayerEventListener {

    private lateinit var rootView: FrameLayout
    private lateinit var container: ConstraintLayout
    private lateinit var questionHeading: AppCompatTextView
    private lateinit var questionDescription: AppCompatTextView
    private lateinit var questionText: DashedUnderlinedTextView
    private lateinit var regularAudioIv: AppCompatImageView
    private lateinit var slowAudioIv: AppCompatImageView
    private lateinit var singleAudioIv: AppCompatImageView
    private lateinit var group1: Group
    private lateinit var group2: Group
    var audioManager: ExoAudioPlayer? = null
    var regularAudio: String? = null
    var slowAudio: String? = null
    var heading: String? = null
    var description: String? = null
    var isNewHeader: Boolean = false

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

    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener = OnTouchListener { v, event ->
        val currentPaddingTop = v.paddingTop
        val currentPaddingBottom = v.paddingBottom
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.setPaddingRelative(
                    v.paddingLeft,
                    currentPaddingTop + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                    v.paddingRight,
                    currentPaddingBottom - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                )
                v.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.setPaddingRelative(
                    v.paddingLeft,
                    currentPaddingTop - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                    v.paddingRight,
                    currentPaddingBottom + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                )
                v.invalidate()
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        View.inflate(context, R.layout.cell_grammar_heading_layout, this)
        rootView = findViewById(R.id.root_view)
        container = findViewById(R.id.container)
        questionHeading = findViewById(R.id.question_heading)
        questionDescription = findViewById(R.id.question_description)
        questionText = findViewById(R.id.question_text)
        regularAudioIv = findViewById(R.id.regular_audio_iv)
        slowAudioIv = findViewById(R.id.slow_audio_iv)
        singleAudioIv = findViewById(R.id.single_audio)
        group1 = findViewById(R.id.group_1)
        group2 = findViewById(R.id.group_2)
        regularAudioIv.setOnClickListener {
            playAudio(regularAudio)
        }
        singleAudioIv.setOnClickListener {
            playAudio(regularAudio)
        }
        slowAudioIv.setOnClickListener {
            playAudio(slowAudio)
        }
        regularAudioIv.setOnTouchListener(onTouchListener)
        slowAudioIv.setOnTouchListener(onTouchListener)
        singleAudioIv.setOnTouchListener(onTouchListener)
        audioManager = ExoAudioPlayer.getInstance()

    }

    fun playAudio(audioUrl: String?) {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) ?: 0 <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(AppObjectController.joshApplication.getString(R.string.volume_up_message))
                .cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }

        audioUrl?.let { url ->
            val audioType = AudioType()
            audioType.audio_url = url
            audioType.downloadedLocalPath = url
            audioType.duration = 1_00
            audioType.id = Random.nextInt().toString()
            onPlayAudio(audioType)
        }
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(
        audioObject: AudioType
    ) {
        audioManager?.playerListener = this
        audioManager?.play(audioObject.audio_url)
    }

    fun setup(
        regularAudio: String?,
        slowAudio: String?,
        heading: String?,
        description: String?,
        isNewHeader: Boolean
    ) {
        this.regularAudio = regularAudio
        this.slowAudio = slowAudio
        this.heading = heading
        this.description = description
        this.isNewHeader = isNewHeader
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }

        if (this.heading.isNullOrBlank()) {
            questionHeading.visibility = View.GONE
        } else {
            questionHeading.visibility = View.VISIBLE
            questionHeading.text = this.heading
        }
        if (this.isNewHeader) {
            group1.visibility = View.GONE
            group2.visibility = View.VISIBLE

            if (this.description.isNullOrBlank()) {
                questionText.visibility = View.GONE
            } else {
                questionText.visibility = View.VISIBLE
                questionText.text = this.description
            }

            if (this.regularAudio.isNullOrBlank()) {
                singleAudioIv.visibility = View.GONE
            } else {
                singleAudioIv.visibility = View.VISIBLE
                playAudio(this.regularAudio)
            }

        } else {
            group1.visibility = View.VISIBLE
            group2.visibility = View.GONE

            if (this.description.isNullOrBlank()) {
                questionDescription.visibility = View.GONE
            } else {
                questionDescription.visibility = View.VISIBLE
                questionDescription.text = this.description
            }

            if (this.slowAudio.isNullOrBlank()) {
                slowAudioIv.visibility = View.GONE
            } else {
                slowAudioIv.visibility = View.VISIBLE
            }

            if (this.regularAudio.isNullOrBlank()) {
                regularAudioIv.visibility = View.GONE
            } else {
                regularAudioIv.visibility = View.VISIBLE
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPausePlayer() {
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (isAudioPlaying()) {
            audioManager?.onPause()
        }
    }
}
