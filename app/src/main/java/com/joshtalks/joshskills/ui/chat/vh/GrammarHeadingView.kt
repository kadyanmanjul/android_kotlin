package com.joshtalks.joshskills.ui.chat.vh

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
import androidx.core.view.ViewCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import kotlin.math.roundToInt
import kotlin.random.Random

class GrammarHeadingView : FrameLayout, AudioPlayerEventListener {

    private lateinit var rootView: FrameLayout //root_view_fl
    private lateinit var container: ConstraintLayout //container
    private lateinit var questionHeading: AppCompatTextView //question_heading
    private lateinit var questionDescription: AppCompatTextView //question_description
    private lateinit var regularAudioIv: AppCompatImageView //regular_audio_iv
    private lateinit var slowAudioIv: AppCompatImageView //slow_audio_iv
    var audioManager = ExoAudioPlayer.getInstance()
    var regularAudio: String? = null
    var slowAudio: String? = null
    var heading: String? = null
    var description: String? = null

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

    val onTouchListener = OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val paddingBottom = v.paddingBottom
                val paddingStart = ViewCompat.getPaddingStart(v)
                val paddingEnd = ViewCompat.getPaddingEnd(v)
                val paddingTop = v.paddingTop
                ViewCompat.setPaddingRelative(
                    v,
                    paddingStart,
                    paddingTop + com.github.mikephil.charting.utils.Utils.convertDpToPixel(3f)
                        .roundToInt(),
                    paddingEnd,
                    paddingBottom
                )
                v.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val paddingBottom = v.paddingBottom
                val paddingStart = ViewCompat.getPaddingStart(v)
                val paddingEnd = ViewCompat.getPaddingEnd(v)
                val paddingTop = v.paddingTop
                ViewCompat.setPaddingRelative(
                    v,
                    paddingStart,
                    paddingTop - com.github.mikephil.charting.utils.Utils.convertDpToPixel(3f)
                        .roundToInt(),
                    paddingEnd,
                    paddingBottom
                )
                v.invalidate()
            }
        }
        false
    }

    private fun init() {
        View.inflate(context, R.layout.cell_grammar_heading_layout, this)
        rootView = findViewById(R.id.root_view)
        container = findViewById(R.id.container)
        questionHeading = findViewById(R.id.question_heading)
        questionDescription = findViewById(R.id.question_description)
        regularAudioIv = findViewById(R.id.regular_audio_iv)
        slowAudioIv = findViewById(R.id.slow_audio_iv)
        regularAudioIv.setOnClickListener {
            playAudio(regularAudio)
        }
        slowAudioIv.setOnClickListener {
            playAudio(slowAudio)
        }
        regularAudioIv.setOnTouchListener(onTouchListener)
        slowAudioIv.setOnTouchListener(onTouchListener)

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
        description: String?
    ) {
        this.regularAudio = regularAudio
        this.slowAudio = slowAudio
        this.heading = heading
        this.description = description
        if (heading.isNullOrBlank()) {
            questionHeading.visibility = View.GONE
        } else {
            questionHeading.visibility = View.VISIBLE
            questionHeading.text = heading
        }

        if (description.isNullOrBlank()) {
            questionDescription.visibility = View.GONE
        } else {
            questionDescription.visibility = View.VISIBLE
            questionDescription.text = description
        }

        if (slowAudio.isNullOrBlank()) {
            slowAudioIv.visibility = View.GONE
        } else {
            slowAudioIv.visibility = View.VISIBLE
        }

        if (regularAudio.isNullOrBlank()) {
            regularAudioIv.visibility = View.GONE
        } else {
            regularAudioIv.visibility = View.VISIBLE
        }

    }
}
