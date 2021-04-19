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
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.extension.slideUpAnimation
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionFeedbackResponse

class GrammarButtonView : FrameLayout {

    private lateinit var rootView: FrameLayout //root_view_fl
    private lateinit var textContainer: ConstraintLayout //text_container
    private lateinit var correctAnswerTitle: AppCompatTextView //correct_answer_title
    private lateinit var correctAnswerDesc: AppCompatTextView //correct_answer_desc
    private lateinit var explanationTitle: AppCompatTextView //explanation_title
    private lateinit var explanationText: AppCompatTextView //explanation_title
    private lateinit var wrongAnswerTitle: AppCompatTextView //wrong_answer_title
    private lateinit var wrongAnswerDesc: AppCompatTextView //wrong_answer_desc
    private lateinit var flagIv: AppCompatImageView //flag_iv
    private lateinit var grammarBtn: MaterialButton //grammar_btn
    private lateinit var wrongAnswerGroup: Group //wrong_anser_group
    private lateinit var rightAnswerGroup: Group //right_answer_group

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
        wrongAnswerGroup = findViewById(R.id.wrong_anser_group)
        rightAnswerGroup = findViewById(R.id.right_answer_group)
        flagIv = findViewById(R.id.flag_iv)
        grammarBtn = findViewById(R.id.grammar_btn)
        grammarBtn.setOnClickListener {
            grammarBtn.text = context.getString(R.string.grammar_btn_text_continue)
            if (checkForCorrectIncorrectLogic()) {
                setCorrectView()
            } else {
                setWrongView()
            }
        }
    }

    private fun checkForCorrectIncorrectLogic(): Boolean {
        //TODO LOGIC
        return true
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        assessmentQuestion.questionFeedback?.run {

            if (this.correctAnswerHeading.isBlank()) {
                correctAnswerTitle.visibility = View.GONE
            } else {
                correctAnswerTitle.visibility = View.VISIBLE
                correctAnswerTitle.text = this.correctAnswerHeading
            }

            if (this.correctAnswerText.isBlank()) {
                correctAnswerDesc.visibility = View.GONE
            } else {
                correctAnswerDesc.visibility = View.VISIBLE
                correctAnswerDesc.text = this.correctAnswerText
            }

            if (this.wrongAnswerHeading.isBlank()) {
                wrongAnswerTitle.visibility = View.GONE
            } else {
                wrongAnswerTitle.visibility = View.VISIBLE
                wrongAnswerTitle.text = this.wrongAnswerHeading
            }

            if (this.wrongAnswerText.isBlank()) {
                wrongAnswerDesc.visibility = View.GONE
            } else {
                wrongAnswerDesc.visibility = View.VISIBLE
                wrongAnswerDesc.text = this.wrongAnswerText
            }

            if (this.wrongAnswerHeading2.isBlank()) {
                explanationTitle.visibility = View.GONE
            } else {
                explanationTitle.visibility = View.VISIBLE
                explanationTitle.text = this.wrongAnswerHeading2
            }

            if (this.wrongAnswerText2.isBlank()) {
                explanationText.visibility = View.GONE
            } else {
                explanationText.visibility = View.VISIBLE
                explanationText.text = this.wrongAnswerText2
            }
        }
    }

    fun setCorrectView() {
        wrongAnswerGroup.visibility = View.GONE
        rightAnswerGroup.visibility = View.VISIBLE
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        //flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        updateBgColor(rootView, R.color.grammar_right_answer_bg)
        updateBgTint(grammarBtn, R.color.grammar_green_color)
        updateImageTint(flagIv, R.color.grammar_green_color)
        textContainer.slideUpAnimation(context)
    }

    fun setWrongView() {
        wrongAnswerGroup.visibility = View.VISIBLE
        rightAnswerGroup.visibility = View.GONE
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.white))
        updateBgColor(rootView, R.color.grammar_wrong_answer_bg)
        updateBgTint(grammarBtn, R.color.grammar_red_color_dark)
        updateImageTint(flagIv, R.color.grammar_red_color_dark)
        //flagIv.backgroundTintList(ContextCompat.getColor(context, R.color.grammar_red_color_dark))
        textContainer.slideUpAnimation(context)
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

}
