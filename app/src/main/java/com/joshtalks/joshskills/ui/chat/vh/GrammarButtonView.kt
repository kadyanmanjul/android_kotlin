package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.extension.slideUpAnimation
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionFeedback
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.util.GrammarButton

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
    private lateinit var grammarBtn: GrammarButton
    private lateinit var wrongAnswerGroup: Group
    private lateinit var rightAnswerGroup: Group
    private var callback: CheckQuestionCallback? = null
    private var isAnswerChecked: Boolean = false
    private var questionFeedback: AssessmentQuestionFeedback? = null

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
        grammarBtn = findViewById(R.id.grammar_btn)
        grammarBtn.setOnClickListener {
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
        /*var booleanA = true
        textContainer.setOnClickListener {
            if (booleanA) {
                enableBtn()
            } else {
                disableBtn()
            }
            booleanA = booleanA.not()
        }*/
    }

    private fun checkForCorrectIncorrectLogic(): Boolean {
        //TODO LOGIC
        return false
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.questionFeedback = assessmentQuestion.questionFeedback

        this.questionFeedback?.run {

            if (this.correctAnswerHeading.isNullOrBlank()) {
                correctAnswerTitle.visibility = View.GONE
            } else {
                correctAnswerTitle.visibility = View.VISIBLE
                correctAnswerTitle.text = this.correctAnswerHeading
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
            textContainer.visibility = View.GONE
            wrongAnswerGroup.visibility = View.GONE
            rightAnswerGroup.visibility = View.GONE

            grammarBtn.isEnabled = false
            grammarBtn.isClickable = false
            grammarBtn.text = context.getString(R.string.grammar_btn_text_check)
            grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_shade_new))
            //updateGrammarButton(grammarBtn, R.color.light_shade_of_gray)

            flagIv.visibility = GONE
            //flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
            updateBgColor(rootView, R.color.white)
            isAnswerChecked = false

        }

    }

    public fun enableBtn() {

        grammarBtn.isEnabled = true
        grammarBtn.isClickable = true
        updateGrammarButton(grammarBtn, R.color.grammar_green_color)
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))

    }

    public fun disableBtn() {

        grammarBtn.isEnabled = false
        grammarBtn.isClickable = false
        updateGrammarButton(grammarBtn, R.color.light_shade_of_gray)
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grey_shade_new))

    }

    fun setCorrectView() {

        wrongAnswerGroup.visibility = View.GONE
        rightAnswerGroup.visibility = View.VISIBLE
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        flagIv.visibility = VISIBLE
        //flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        updateBgColor(rootView, R.color.grammar_right_answer_bg)
        updateGrammarButton(grammarBtn, R.color.grammar_green_color)
        updateImageTint(flagIv, R.color.grammar_green_color)
        textContainer.slideUpAnimation(context)

    }

    fun setWrongView() {

        wrongAnswerGroup.visibility = View.VISIBLE
        rightAnswerGroup.visibility = View.GONE
        flagIv.visibility = VISIBLE
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        updateBgColor(rootView, R.color.grammar_wrong_answer_bg)
        updateGrammarButton(grammarBtn, R.color.grammar_red_color_dark)
        updateImageTint(flagIv, R.color.grammar_red_color_dark)
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

    private fun updateGrammarButton(
        grammarBtn: GrammarButton,
        buttonColor: Int,
        shadowColor: Int = R.color.light_shade_of_gray
    ) {
        //grammarBtn.backgroundTintList = ContextCompat.getColorStateList(context, buttonColor)
        grammarBtn.buttonColor = buttonColor
        grammarBtn.shadowColor = shadowColor
        grammarBtn.isShadowEnabled = true
        //grammarBtn.refresh()
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
    }

}
