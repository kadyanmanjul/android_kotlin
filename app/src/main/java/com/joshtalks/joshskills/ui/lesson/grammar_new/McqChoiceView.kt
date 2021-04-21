package com.joshtalks.joshskills.ui.lesson.grammar_new

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.ui.chat.vh.EnableDisableGrammarButtonCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class McqChoiceView : RadioGroup {

    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    lateinit var mcqOptionsRadioGroup: RadioGroup
    private var callback: EnableDisableGrammarButtonCallback? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.mcq_option_group, this)
        mcqOptionsRadioGroup = findViewById(R.id.mcq_options_radio_group)
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        mcqOptionsRadioGroup.removeAllViews()
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                val optionView = getOptionView(it)
                mcqOptionsRadioGroup.addView(optionView)
            }
        CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
            delay(500)
            if (assessmentQuestion.question.isAttempted) {
                callback?.alreadyAttempted(isCorrectAnswer())
            }
        }
    }

    private fun getOptionView(choice: Choice): McqOptionView {
        val optionView = McqOptionView(context, choice)
        optionView.setOnClickListener(ClickListener())
        return optionView
    }

    private fun lockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).setEnabled(false)
        }
    }

    private fun unlockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).setEnabled(true)
        }
    }

    fun isAnyAnswerSelected(): Boolean {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            if ((mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).choice.isSelectedByUser) {
                return true
            }
        }
        return false
    }

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            assessmentQuestion?.question?.isAttempted = true
            assessmentQuestion?.choiceList?.forEach {
                if (it.isSelectedByUser != it.isCorrect) {
                    return false
                }
            }
        }
        return true
    }

    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

    private inner class ClickListener : View.OnClickListener {
        override fun onClick(view: View?) {
            val clickedOptionView = view as McqOptionView?
            for (i in 0 until mcqOptionsRadioGroup.childCount) {
                val optionView = (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView)
                if (optionView == clickedOptionView) {
                    optionView.changeState()
                } else {
                    optionView.setState(McqOptionState.UNSELECTED)
                }
                if (isAnyAnswerSelected()) {
                    callback?.enableGrammarButton()
                } else {
                    callback?.disableGrammarButton()
                }
            }
        }
    }

}
