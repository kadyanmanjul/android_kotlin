package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatEditText
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubjectiveChoiceView : FrameLayout, TextWatcher {

    private lateinit var rootView: FrameLayout
    private lateinit var answerText: AppCompatEditText
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var callback: EnableDisableGrammarButtonCallback? = null

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
    }


    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        answerText.text?.clear()

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
            delay(500)
            if (assessmentQuestion.question.isAttempted) {
                callback?.alreadyAttempted(isCorrectAnswer())
            }
        }
    }

    private fun lockViews() {
        answerText.setFocusable(false)
        answerText.setEnabled(false)
        answerText.setCursorVisible(false)
        answerText.addTextChangedListener(null)
        answerText.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun unlockViews() {
        answerText.setFocusable(true)
        answerText.setEnabled(true)
        answerText.setCursorVisible(true)
        answerText.addTextChangedListener(this)
        answerText.setBackgroundColor(Color.BLACK)
    }

    fun isAnyAnswerSelected() = answerText.text?.length ?: 0 > 0

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            return answerText.text?.equals(assessmentQuestion?.questionFeedback?.correctAnswerText)
                ?: false
        }
    }

    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        if (isAnyAnswerSelected()) {
            callback?.enableGrammarButton()
        } else {
            callback?.disableGrammarButton()
        }
    }

    override fun afterTextChanged(p0: Editable?) {
    }

}