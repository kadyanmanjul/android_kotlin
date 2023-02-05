package com.joshtalks.joshskills.premium.ui.online_test.vh

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.doOnTextChanged
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.premium.ui.online_test.util.AssessmentQuestionViewCallback
import com.joshtalks.joshskills.premium.ui.online_test.util.GrammarSubmitButtonListener
import java.util.*

class SubjectiveChoiceView : FrameLayout, AssessmentQuestionViewCallback {

    private var grammarTestListener: GrammarSubmitButtonListener? = null
    private lateinit var rootView: FrameLayout
    private lateinit var answerText: AppCompatEditText
    override var assessmentQuestion: AssessmentQuestionWithRelations? = null

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
        View.inflate(context, R.layout.grammar_subjective_choice_view, this)
        rootView = findViewById(R.id.root_view)
        answerText = findViewById(R.id.submitted_answer_text)
        answerText.doOnTextChanged { _, _, _, count ->
            grammarTestListener?.toggleSubmitButton(count != 0)
        }
    }

    override fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        answerText.text?.clear()
        unlockViews()
    }

    override fun lockViews() {
        answerText.isEnabled = false
        answerText.isCursorVisible = false
        answerText.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun unlockViews() {
        answerText.isEnabled = true
        answerText.isCursorVisible = true
        answerText.setBackgroundColor(Color.BLACK)
        answerText.requestFocus()
    }

    override fun isAnyAnswerSelected() = (answerText.text?.length ?: 0) > 0

    override fun isAnswerCorrect(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            val inputText = getAnswerText()
            assessmentQuestion?.question?.isAttempted = true
            assessmentQuestion?.choiceList?.filter {
                it.text.toString().trim().equals(inputText, true)
            }.also {
                assessmentQuestion?.question?.isCorrect = !it.isNullOrEmpty()
                return it?.isNotEmpty() ?: false
            }
        }
    }

    override fun addGrammarTestCallback(callback: GrammarSubmitButtonListener) {
        grammarTestListener = callback
    }

    override fun getAnswerText(): String =
        answerText.text.toString().lowercase(Locale.getDefault()).trim()
}