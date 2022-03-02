package com.joshtalks.joshskills.ui.chat.vh

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.extension.slideUpAnimation
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionFeedback
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.ui.lesson.LessonActivity

class GrammarButtonView : FrameLayout {

    private lateinit var rootView: FrameLayout
    private lateinit var textContainer: ConstraintLayout
    private lateinit var correctAnswerTitle: AppCompatTextView
    private lateinit var correctAnswerDesc: AppCompatTextView
    private lateinit var explanationTitle: AppCompatTextView
    private lateinit var explanationText: AppCompatTextView
    private lateinit var wrongAnswerTitle: AppCompatTextView
    private lateinit var wrongAnswerDesc: AppCompatTextView
    private lateinit var flagIv: AppCompatImageView
    private lateinit var videoIv: AppCompatImageView
    private lateinit var animatedVideoIv: LottieAnimationView
    private lateinit var grammarBtn: MaterialTextView
    private lateinit var wrongAnswerGroup: Group
    private lateinit var rightAnswerGroup: Group
    private var callback: CheckQuestionCallback? = null
    private var isAnswerChecked: Boolean = false
    private var questionFeedback: AssessmentQuestionFeedback? = null
    private var reviseVideoObject: VideoModel? = null
    private var currentState: GrammarButtonState = GrammarButtonState.DISABLED

    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener3 = OnTouchListener { v, event ->
        val currentPaddingTop = v.paddingTop
        val currentPaddingBottom = v.paddingBottom
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val drawable =
                    when (currentState) {
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

                v.background = androidx.core.content.ContextCompat.getDrawable(
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

                v.background = androidx.core.content.ContextCompat.getDrawable(
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
        flagIv = findViewById(R.id.flag_iv)
        videoIv = findViewById(R.id.video_iv)
        animatedVideoIv = findViewById(R.id.animated_video_iv)
        grammarBtn = findViewById(R.id.grammar_btn)
        setGrammarButtonListners()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setGrammarButtonListners() {
        grammarBtn.setOnClickListener {
            if (LessonActivity.isVideoVisible.value == true)
                LessonActivity.isVideoVisible.value = false
            if (isAnswerChecked) {
                callback?.nextQuestion()
            } else {
                grammarBtn.text = context.getString(R.string.grammar_btn_text_continue)
                callback?.let { callback ->
                    val result = callback.checkQuestionCallBack()
                    if (result != null) {
                        isAnswerChecked = true
                        if (result) {
                            setCorrectView()
                        } else {
                            setWrongView()
                        }
                    } else {
                        disableBtn()
                    }
                }
            }
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
        if (reviseVideoObject?.video_url.isNullOrBlank().not()) {
            callback?.onVideoButtonAppear(
                isClicked = true,
                wrongAnswerHeading = this.questionFeedback?.wrongAnswerHeading,
                wrongAnswerHeading2 = this.questionFeedback?.wrongAnswerHeading2,
                wrongAnswerText1 = this.questionFeedback?.wrongAnswerText,
                wrongAnswerText2 = this.questionFeedback?.wrongAnswerText2,
            )
            openVideoObject()
            animatedVideoIv.visibility = GONE
            videoIv.visibility = VISIBLE
        }
    }

    private fun openVideoObject() {
        if (reviseVideoObject?.video_url.isNullOrBlank().not()) {
            RxBus2.publish(
                VideoShowEvent(
                    videoTitle = EMPTY,
                    videoId = reviseVideoObject?.id,
                    videoUrl = reviseVideoObject?.video_url,
                    videoWidth = reviseVideoObject?.video_width ?: 0,
                    videoHeight = reviseVideoObject?.video_height ?: 0
                )
            )
        }
    }

    private fun checkForCorrectIncorrectLogic(): Boolean {
        //TODO LOGIC
        return false
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations, reviseVideoObject: VideoModel?) {
        this.questionFeedback = assessmentQuestion.questionFeedback
        this.reviseVideoObject = reviseVideoObject

        this.questionFeedback?.run {

            if (this.correctAnswerHeading.isNullOrBlank()) {
                correctAnswerTitle.visibility = View.GONE
            } else {
                correctAnswerTitle.visibility = View.VISIBLE
                correctAnswerTitle.text = this.correctAnswerHeading
                //correctAnswerTitle.text = getFormattedText(this.correctAnswerHeading)
            }

            if (this.correctAnswerText.isNullOrBlank()) {
                correctAnswerDesc.visibility = View.GONE
            } else {
                correctAnswerDesc.visibility = View.VISIBLE
                correctAnswerDesc.text = this.correctAnswerText
            }

            if (this.wrongAnswerHeading.isNullOrBlank()) {
                wrongAnswerTitle.visibility = View.GONE
            } else {
                wrongAnswerTitle.visibility = View.VISIBLE
                wrongAnswerTitle.text = this.wrongAnswerHeading
//                wrongAnswerTitle.text = getFormattedText(this.wrongAnswerHeading)
            }

            if (this.wrongAnswerText.isNullOrBlank()) {
                wrongAnswerDesc.visibility = View.GONE
            } else {
                wrongAnswerDesc.visibility = View.VISIBLE
                wrongAnswerDesc.text = this.wrongAnswerText
            }

            if (this.wrongAnswerHeading2.isNullOrBlank()) {
                explanationTitle.visibility = View.GONE
            } else {
                explanationTitle.visibility = View.VISIBLE
                explanationTitle.text = this.wrongAnswerHeading2
            }

            if (this.wrongAnswerText2.isNullOrBlank()) {
                explanationText.visibility = View.GONE
            } else {
                explanationText.visibility = View.VISIBLE
                explanationText.text = this.wrongAnswerText2
            }
        }
        textContainer.visibility = View.GONE
        wrongAnswerGroup.visibility = View.GONE
        rightAnswerGroup.visibility = View.GONE

        grammarBtn.isEnabled = false
        grammarBtn.isClickable = false
        grammarBtn.text = context.getString(R.string.grammar_btn_text_check)
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_shade_new))
        currentState = GrammarButtonState.DISABLED
        updateGrammarButtonDrawable(grammarBtn, R.drawable.gray_btn_pressed_state)
        flagIv.visibility = GONE
        videoIv.visibility = GONE
        //flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        updateBgColor(rootView, R.color.white)
        isAnswerChecked = false

    }

    //private fun getFormattedText(string : String) = string.replace("\\n", "\n").trim()

    fun enableBtn() {

        grammarBtn.isEnabled = true
        grammarBtn.isClickable = true
        currentState = GrammarButtonState.ENABLED
        updateGrammarButtonDrawable(grammarBtn, R.drawable.green_btn_grammar_selector)
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))

    }

    fun disableBtn() {

        grammarBtn.isEnabled = false
        grammarBtn.isClickable = false
        currentState = GrammarButtonState.DISABLED
        updateGrammarButtonDrawable(grammarBtn, R.drawable.gray_btn_pressed_state)
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_shade_new))

    }

    fun setCorrectView() {

        wrongAnswerGroup.visibility = View.GONE
        rightAnswerGroup.visibility = View.VISIBLE
        setCorrectViewVisibility()
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        flagIv.visibility = View.GONE
        currentState = GrammarButtonState.CORRECT
        //flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        updateBgColor(rootView, R.color.grammar_right_answer_bg)
        updateGrammarButtonDrawable(grammarBtn, R.drawable.green_btn_grammar_selector)
//        updateImageTint(flagIv, R.color.grammar_green_color)
        animatedVideoIv.visibility = GONE

        if (reviseVideoObject?.video_url.isNullOrBlank()) {
            videoIv.visibility = GONE
        } else {
            videoIv.visibility = VISIBLE
//            if (PrefManager.getBoolValue(HAS_SEEN_QUIZ_VIDEO_BUTTON))
            if (PrefManager.isInSet(
                    key = LAST_SEEN_VIDEO_ID,
                    value = reviseVideoObject?.id ?: EMPTY,
                    isConsistent = false,
                )
            ) {
                videoIv.setImageResource(R.drawable.ic_video_seen)
            } else {
                videoIv.setImageResource(R.drawable.ic_video_clip)
            }
            updateImageTint(videoIv, R.color.grammar_green_color)
        }
        textContainer.slideUpAnimation(context)

    }

    private fun setCorrectViewVisibility() {

        this.questionFeedback?.run {

            if (this.correctAnswerHeading.isNullOrBlank()) {
                correctAnswerTitle.visibility = View.GONE
            }

            if (this.correctAnswerText.isNullOrBlank()) {
                correctAnswerDesc.visibility = View.GONE
            }
        }
    }

    private fun setWrongViewVisibility() {

        this.questionFeedback?.run {

            if (this.wrongAnswerHeading.isNullOrBlank()) {
                wrongAnswerTitle.visibility = View.GONE
            }

            if (this.wrongAnswerText.isNullOrBlank()) {
                wrongAnswerDesc.visibility = View.GONE
            }

            if (this.wrongAnswerHeading2.isNullOrBlank()) {
                explanationTitle.visibility = View.GONE
            }

            if (this.wrongAnswerText2.isNullOrBlank()) {
                explanationText.visibility = View.GONE
            }
        }
    }

    fun setWrongView() {
        wrongAnswerGroup.visibility = View.VISIBLE
        rightAnswerGroup.visibility = View.GONE
        setWrongViewVisibility()
        flagIv.visibility = View.GONE
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        updateBgColor(rootView, R.color.grammar_wrong_answer_bg)
        currentState = GrammarButtonState.WRONG
        updateGrammarButtonDrawable(grammarBtn, R.drawable.red_btn_grammar_selector)
//        updateImageTint(flagIv, R.color.grammar_red_color_dark)
        if (reviseVideoObject?.video_url.isNullOrBlank()) {
            videoIv.visibility = GONE
            animatedVideoIv.visibility = GONE
        } else {
            if (PrefManager.isInSet(
                    key = LAST_SEEN_VIDEO_ID,
                    value = reviseVideoObject?.id ?: EMPTY,
                    isConsistent = false,
                )
            ) {
                animatedVideoIv.visibility = GONE
                videoIv.visibility = VISIBLE
                videoIv.setImageResource(R.drawable.ic_video_seen)
            } else {
                videoIv.visibility = INVISIBLE
                animatedVideoIv.visibility = VISIBLE
            }
            callback?.onVideoButtonAppear(
                isClicked = false,
                wrongAnswerHeading = this.questionFeedback?.wrongAnswerHeading,
                wrongAnswerHeading2 = this.questionFeedback?.wrongAnswerText,
                wrongAnswerText1 = questionFeedback?.wrongAnswerText,
                wrongAnswerText2 = questionFeedback?.wrongAnswerText2
            )
            updateImageTint(videoIv, R.color.grammar_red_color_dark)
        }
        textContainer.slideUpAnimation(context)

    }

    fun setAlreadyAttemptedView(isCorrect: Boolean) {
        grammarBtn.text = context.getString(R.string.grammar_btn_text_continue)
        isAnswerChecked = true
        enableBtn()
        if (isCorrect) {
            setCorrectView()
        } else
            setWrongView()
    }

    private fun updateGrammarButtonDrawable(
        grammarBtn: MaterialTextView,
        drawable: Int
    ) {
        grammarBtn.setViewBackgroundWithoutResettingPadding(drawable)

    }

    fun View.setViewBackgroundWithoutResettingPadding(@DrawableRes backgroundResId: Int) {
        val paddingBottom = this.paddingBottom
        val paddingStart = ViewCompat.getPaddingStart(this)
        val paddingEnd = ViewCompat.getPaddingEnd(this)
        val paddingTop = this.paddingTop
        setBackgroundResource(backgroundResId)
        ViewCompat.setPaddingRelative(this, paddingStart, paddingTop, paddingEnd, paddingBottom)
    }

    private fun updateGrammarButton(
        grammarBtn: MaterialTextView,
        buttonColor: Int
    ) {
        //grammarBtn.backgroundTintList = ContextCompat.getColorStateList(context, buttonColor)
    }

    private fun updateBgTint(view: View, color: Int) {
        view.backgroundTintList = ContextCompat.getColorStateList(context, color)
    }

    private fun updateImageTint(view: AppCompatImageView, color: Int) {
        view.imageTintList = ContextCompat.getColorStateList(context, color)
    }

    private fun updateBgColor(view: View, color: Int) {

        view.setBackgroundColor(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                color
            )
        )

    }

    fun addCallback(callback: CheckQuestionCallback) {
        this.callback = callback
    }

    interface CheckQuestionCallback {
        fun checkQuestionCallBack(): Boolean?
        fun nextQuestion()
        fun onVideoButtonAppear(
            isClicked: Boolean,
            wrongAnswerHeading: String?,
            wrongAnswerHeading2: String?,
            wrongAnswerText1: String?,
            wrongAnswerText2: String?
        )
    }

    enum class GrammarButtonState(state: String) {
        DISABLED("DISABLED"),
        ENABLED("DISABLED"),
        CORRECT("CORRECT"),
        WRONG("WRONG")
    }

}
