package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.extension.slideInAnimation
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionFeedbackResponse

class GrammarButtonView : FrameLayout {


    private lateinit var rootView: FrameLayout //root_view_fl
    private lateinit var textContainer: FrameLayout //text_container
    private lateinit var correctAnswerTitle: AppCompatTextView //correct_answer_title
    private lateinit var correctAnswerDesc: AppCompatTextView //correct_answer_desc
    private lateinit var explanationTitle: AppCompatTextView //explanation_title
    private lateinit var explanationText: AppCompatTextView //explanation_text
    private lateinit var flagIv: AppCompatImageView //flag_iv
    private lateinit var grammarBtn: MaterialButton //grammar_btn

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
        rootView = findViewById(R.id.root_view_fl)
        textContainer = findViewById(R.id.text_container)
        correctAnswerTitle = findViewById(R.id.correct_answer_title)
        correctAnswerDesc = findViewById(R.id.correct_answer_desc)
        explanationTitle = findViewById(R.id.explanation_title)
        explanationText = findViewById(R.id.explanation_text)
        flagIv = findViewById(R.id.flag_iv)
        grammarBtn = findViewById(R.id.grammar_btn)
        grammarBtn.setOnClickListener {
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

    fun setup(assessmentQuestionFeedbackResponse: AssessmentQuestionFeedbackResponse) {
        assessmentQuestionFeedbackResponse.run {
            correctAnswerTitle.text=this.correctAnswerHeading
            correctAnswerDesc.text=this.correctAnswerText
            explanationTitle .text=this.wrongAnswerText
            explanationText.text=this.wrongAnswerText2
        }

    }

    fun setCorrectView() {
        correctAnswerTitle.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.grammar_green_color
            )
        )
        correctAnswerDesc.setTextColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grammar_green_color_btn))
        flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_green_color))
        textContainer.slideInAnimation()
    }

    fun setWrongView() {
        explanationTitle.visibility = View.VISIBLE
        explanationText.visibility = View.VISIBLE
        correctAnswerTitle.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.grammar_red_color_dark
            )
        )
        correctAnswerDesc.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.grammar_red_color_light
            )
        )
        grammarBtn.setTextColor(ContextCompat.getColor(context, R.color.grammar_red_color_btn))
        flagIv.setBackgroundColor(ContextCompat.getColor(context, R.color.grammar_red_color_dark))
        textContainer.slideInAnimation()
    }

}