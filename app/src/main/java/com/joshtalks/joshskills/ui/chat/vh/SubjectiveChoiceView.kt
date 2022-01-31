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
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SubjectiveChoiceView : FrameLayout {

    private lateinit var rootView: FrameLayout
    private lateinit var answerText: AppCompatEditText
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var callback: EnableDisableGrammarButtonCallback? = null
    val textListner = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
            assessmentQuestion?.choiceList?.get(0)?.imageUrl = s.toString()
            if (s.toString().isNullOrBlank()) {
                callback?.disableGrammarButton()
            } else {
                callback?.enableGrammarButton()
            }
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun afterTextChanged(p0: Editable?) {

        }
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
        View.inflate(context, R.layout.grammar_subjective_choice_view, this)
        rootView = findViewById(R.id.root_view)
        answerText = findViewById(R.id.submitted_answer_text)
        answerText.addTextChangedListener(textListner)
    }


    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        answerText.removeTextChangedListener(textListner)
        answerText.text?.clear()
        unlockViews()

        CoroutineScope(Dispatchers.IO).launch(Dispatchers.Main) {
            delay(500)
            if (assessmentQuestion.question.isAttempted) {
                callback?.alreadyAttempted(isCorrectAnswer())
            }
        }
    }

    private fun lockViews() {
        answerText.setEnabled(false)
        answerText.setCursorVisible(false)
        answerText.removeTextChangedListener(textListner)
        answerText.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun unlockViews() {
        answerText.setEnabled(true)
        answerText.setCursorVisible(true)
        answerText.addTextChangedListener(textListner)
        answerText.setBackgroundColor(Color.BLACK)
        answerText.requestFocus()
    }

    fun isAnyAnswerSelected() = answerText.text?.length ?: 0 > 0

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            val inputText = answerText.text.toString().lowercase(Locale.getDefault()).trim()
//            return inputText.equals(assessmentQuestion?.choiceList?.get(0)?.text.toString().toLowerCase(Locale.getDefault()))
            assessmentQuestion?.choiceList?.filter {
                it.text.toString().trim().lowercase(Locale.getDefault()).equals(inputText, true)
            }.also {
                return it?.isNotEmpty() ?: false
            }
        }
    }

    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

}