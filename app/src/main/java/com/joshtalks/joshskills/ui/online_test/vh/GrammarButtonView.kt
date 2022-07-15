package com.joshtalks.joshskills.ui.online_test.vh

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.extension.slideUpAnimation
import com.joshtalks.joshskills.base.local.model.assessment.AssessmentQuestionFeedback
import com.joshtalks.joshskills.base.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.ui.online_test.util.GrammarButtonViewCallback

class GrammarButtonView : FrameLayout {

    private lateinit var rootView: FrameLayout
    private lateinit var textContainer: ConstraintLayout
    private lateinit var correctAnswerTitle: AppCompatTextView
    private lateinit var correctAnswerDesc: AppCompatTextView
    private lateinit var explanationTitle: AppCompatTextView
    private lateinit var explanationText: AppCompatTextView
    private lateinit var wrongAnswerTitle: AppCompatTextView
    private lateinit var wrongAnswerDesc: AppCompatTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var videoIv: AppCompatImageView
    private lateinit var animatedVideoIv: LottieAnimationView
    private lateinit var grammarBtn: MaterialTextView
    private lateinit var wrongAnswerGroup: Group
    private lateinit var rightAnswerGroup: Group
    private var callback: GrammarButtonViewCallback? = null
    private var questionFeedback: AssessmentQuestionFeedback? = null
    private var videoId: String? = null
    private var currentState: GrammarButtonState = GrammarButtonState.DISABLED

    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener3 = OnTouchListener { v, event ->
        val currentPaddingTop = v.paddingTop
        val currentPaddingBottom = v.paddingBottom
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val drawable =
                    when (currentState) {
                        GrammarButtonState.LOADING,
                        GrammarButtonState.DISABLED -> {
                            R.drawable.gray_btn_pressed_state
                        }
                        GrammarButtonState.CORRECT, GrammarButtonState.ENABLED -> {
                            R.drawable.green_btn_pressed_state
                        }
                        GrammarButtonState.WRONG -> {
                            R.drawable.red_btn_pressed_state
                        }
                    }

                v.background = ContextCompat.getDrawable(
                    context,
                    drawable
                )

                v.setPaddingRelative(
                    v.paddingLeft,
                    currentPaddingTop + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                    v.paddingRight,
                    currentPaddingBottom - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                )
                v.invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                val drawable =
                    when (currentState) {
                        GrammarButtonState.LOADING,
                        GrammarButtonState.DISABLED -> {
                            R.drawable.gray_btn_pressed_state
                        }
                        GrammarButtonState.CORRECT, GrammarButtonState.ENABLED -> {
                            R.drawable.green_btn_unpressed_state
                        }
                        GrammarButtonState.WRONG -> {
                            R.drawable.red_btn_unpressed_state
                        }
                    }

                v.background = ContextCompat.getDrawable(
                    context,
                    drawable
                )
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
        View.inflate(context, R.layout.cell_grammar_button_layout, this)
        rootView = findViewById(R.id.root_view)
        textContainer = findViewById(R.id.text_container)
        correctAnswerTitle = findViewById(R.id.correct_answer_title)
        correctAnswerDesc = findViewById(R.id.correct_answer_desc)
        explanationTitle = findViewById(R.id.explanation_title)
        explanationText = findViewById(R.id.explanation_text)
        wrongAnswerTitle = findViewById(R.id.wrong_answer_title)
        wrongAnswerDesc = findViewById(R.id.wrong_answer_desc)
        wrongAnswerGroup = findViewById(R.id.wrong_answer_group)
        rightAnswerGroup = findViewById(R.id.right_answer_group)
        videoIv = findViewById(R.id.video_iv)
        animatedVideoIv = findViewById(R.id.animated_video_iv)
        grammarBtn = findViewById(R.id.grammar_btn)
        progressBar = findViewById(R.id.progressBar)
        setGrammarButtonListeners()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setGrammarButtonListeners() {
        grammarBtn.setOnClickListener {
            callback?.onGrammarButtonClick()
        }
        grammarBtn.setOnTouchListener(onTouchListener3)
        videoIv.setOnClickListener {
            viewVideo()
        }
        animatedVideoIv.setOnClickListener {
            viewVideo()
        }
    }

    fun viewVideo() {
        videoId?.let {
            callback?.onVideoButtonClicked()
            animatedVideoIv.visibility = GONE
            videoIv.visibility = VISIBLE
        }
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations, videoId: String? = null) {
        this.questionFeedback = assessmentQuestion.questionFeedback
        this.videoId = videoId
        this.questionFeedback?.run {

            correctAnswerTitle.isVisible = this.correctAnswerHeading.isNullOrEmpty().not()
            correctAnswerTitle.text = this.correctAnswerHeading

            correctAnswerDesc.isVisible = this.correctAnswerText.isNullOrEmpty().not()
            correctAnswerDesc.text = this.correctAnswerText

            wrongAnswerTitle.isVisible = this.wrongAnswerHeading.isNullOrEmpty().not()
            wrongAnswerTitle.text = this.wrongAnswerHeading

            wrongAnswerDesc.isVisible = this.wrongAnswerText.isNullOrEmpty().not()
            wrongAnswerDesc.text = this.wrongAnswerText

            explanationTitle.isVisible = this.wrongAnswerHeading2.isNullOrEmpty().not()
            explanationTitle.text = this.wrongAnswerHeading2

            explanationText.isVisible = this.wrongAnswerText2.isNullOrEmpty().not()
            explanationText.text = this.wrongAnswerText2
        }

        textContainer.visibility = View.GONE
        wrongAnswerGroup.visibility = View.GONE
        rightAnswerGroup.visibility = View.GONE
        grammarBtn.isEnabled = false
        grammarBtn.isClickable = false
        grammarBtn.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.GRAMMAR_CHECK_BUTTON_TEXT + PrefManager.getStringValue(
                CURRENT_COURSE_ID,
                false,
                DEFAULT_COURSE_ID
            )
        )
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_shade_new))
        currentState = GrammarButtonState.DISABLED
        grammarBtn.setViewBackgroundWithoutResettingPadding(R.drawable.gray_btn_pressed_state)
        videoIv.visibility = GONE
        animatedVideoIv.visibility = GONE
        progressBar.visibility = View.GONE
        rootView.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
    }

    fun showAnswerFeedbackView(isCorrectAnswer: Boolean) {
        rightAnswerGroup.isVisible = isCorrectAnswer
        wrongAnswerGroup.isVisible = isCorrectAnswer.not()
        if (isCorrectAnswer)
            setCorrectViewVisibility()
        else
            setWrongViewVisibility()
        currentState =
            if (isCorrectAnswer)
                GrammarButtonState.CORRECT
            else
                GrammarButtonState.WRONG
        grammarBtn.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.GRAMMAR_CONTINUE_BUTTON_TEXT +
                    PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        rootView.setBackgroundColor(
            ContextCompat.getColor(
                context,
                if (isCorrectAnswer)
                    R.color.grammar_right_answer_bg
                else
                    R.color.grammar_wrong_answer_bg
            )
        )
        grammarBtn.setViewBackgroundWithoutResettingPadding(
            if (isCorrectAnswer)
                R.drawable.green_btn_grammar_selector
            else R.drawable.red_btn_grammar_selector
        )
        progressBar.setViewBackgroundWithoutResettingPadding(
            if (isCorrectAnswer)
                R.drawable.green_btn_pressed_state
            else R.drawable.red_btn_pressed_state
        )

        if (videoId.isNullOrEmpty()) {
            videoIv.visibility = GONE
            animatedVideoIv.visibility = GONE
        } else {
            videoIv.imageTintList = ContextCompat.getColorStateList(
                context,
                if (isCorrectAnswer) R.color.grammar_green_color
                else R.color.grammar_red_color_dark
            )
            if (hasUserSeenVideo()) {
                animatedVideoIv.visibility = GONE
                videoIv.visibility = VISIBLE
                videoIv.setImageResource(R.drawable.ic_video_seen)
            } else {
                if (isCorrectAnswer) {
                    videoIv.visibility = VISIBLE
                    animatedVideoIv.visibility = GONE
                    videoIv.setImageResource(R.drawable.ic_video_clip)
                } else {
                    videoIv.visibility = INVISIBLE
                    animatedVideoIv.visibility = VISIBLE
                    callback?.showTooltip(
                        wrongAnswerTitle = this.questionFeedback?.wrongAnswerHeading,
                        explanationTitle = this.questionFeedback?.wrongAnswerHeading2,
                        explanationText = questionFeedback?.wrongAnswerText,
                        wrongAnswerDescription = questionFeedback?.wrongAnswerText2
                    )
                }
            }
        }
        textContainer.slideUpAnimation(context)
    }

    private fun hasUserSeenVideo() =
        PrefManager.isInSet(
            key = LAST_SEEN_VIDEO_ID,
            value = videoId!!,
            isConsistent = false
        )

    private fun setCorrectViewVisibility() {
        questionFeedback?.run {
            correctAnswerTitle.isVisible = !correctAnswerHeading.isNullOrEmpty()
            correctAnswerDesc.isVisible = !correctAnswerHeading.isNullOrEmpty()
        }
    }

    private fun setWrongViewVisibility() {
        questionFeedback?.run {
            wrongAnswerTitle.isVisible = !wrongAnswerHeading.isNullOrBlank()
            wrongAnswerDesc.isVisible = !wrongAnswerText.isNullOrBlank()
            explanationTitle.isVisible = !wrongAnswerHeading2.isNullOrBlank()
            explanationText.isVisible = !wrongAnswerText2.isNullOrBlank()
        }
    }

    fun View.setViewBackgroundWithoutResettingPadding(@DrawableRes backgroundResId: Int) {
        val paddingBottom = this.paddingBottom
        val paddingStart = ViewCompat.getPaddingStart(this)
        val paddingEnd = ViewCompat.getPaddingEnd(this)
        val paddingTop = this.paddingTop
        setBackgroundResource(backgroundResId)
        ViewCompat.setPaddingRelative(this, paddingStart, paddingTop, paddingEnd, paddingBottom)
    }

    fun addCallback(callback: GrammarButtonViewCallback) {
        this.callback = callback
    }

    fun toggleLoading(isLoading: Boolean = false) {
        toggleSubmitButton(isLoading.not())
        progressBar.isVisible = isLoading
    }

    fun toggleSubmitButton(enabled: Boolean) {
        grammarBtn.isEnabled = enabled
        grammarBtn.isClickable = enabled
        currentState =
            if (enabled)
                GrammarButtonState.ENABLED
            else
                GrammarButtonState.DISABLED
        grammarBtn.setViewBackgroundWithoutResettingPadding(
            if (enabled)
                R.drawable.green_btn_grammar_selector
            else R.drawable.gray_btn_pressed_state
        )
        grammarBtn.setTextColor(
            ContextCompat.getColor(
                context,
                if (enabled) R.color.white
                else R.color.grey_shade_new
            )
        )
    }

    enum class GrammarButtonState {
        DISABLED,
        ENABLED,
        CORRECT,
        WRONG,
        LOADING;
    }

}
