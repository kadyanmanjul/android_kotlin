package com.joshtalks.joshskills.lesson.online_test.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RadioGroup
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.common.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.lesson.online_test.vh.McqOptionState
import com.joshtalks.joshskills.lesson.online_test.vh.McqOptionView
import com.joshtalks.joshskills.lesson.online_test.util.AssessmentQuestionViewCallback
import com.joshtalks.joshskills.lesson.online_test.util.GrammarSubmitButtonListener

class McqChoiceView : RadioGroup, AssessmentQuestionViewCallback {

    override var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var grammarTestListener: GrammarSubmitButtonListener? = null
    lateinit var mcqOptionsRadioGroup: RadioGroup

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

    override fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        mcqOptionsRadioGroup.removeAllViews()
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                val optionView = getOptionView(it)
                mcqOptionsRadioGroup.addView(optionView)
            }
        unlockViews()
    }

    private fun getOptionView(choice: Choice): McqOptionView {
        val optionView = McqOptionView(context, choice)
        optionView.setOnClickListener(ClickListener())
        return optionView
    }

    override fun lockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount)
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).isEnabled = false
    }

    override fun unlockViews() {
        for (i in 0 until mcqOptionsRadioGroup.childCount)
            (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).isEnabled = true
    }

    override fun getAnswerText(): String {
        assessmentQuestion?.choiceList?.filter { it.isSelectedByUser }?.let {
            return it.first().text ?: ""
        } ?: return ""
    }

    override fun isAnyAnswerSelected(): Boolean {
        for (i in 0 until mcqOptionsRadioGroup.childCount) {
            if ((mcqOptionsRadioGroup.getChildAt(i) as McqOptionView).choice.isSelectedByUser)
                return true
        }
        return false
    }

    fun checkAnswerInList(): Boolean {
        assessmentQuestion?.choiceList?.forEach {
            if (it.isSelectedByUser != it.isCorrect)
                return false
        }
        return true
    }

    override fun isAnswerCorrect(): Boolean {
        lockViews()
        return if (isAnyAnswerSelected().not()) {
            unlockViews()
            false
        } else {
            checkAnswerInList().also {
                assessmentQuestion?.question?.isCorrect = it
            }
        }
    }

    override fun addGrammarTestCallback(callback: GrammarSubmitButtonListener) {
        grammarTestListener = callback
    }

    private inner class ClickListener : OnClickListener {
        override fun onClick(view: View?) {
            for (i in 0 until mcqOptionsRadioGroup.childCount) {
                val optionView = (mcqOptionsRadioGroup.getChildAt(i) as McqOptionView)
                if (optionView == (view as McqOptionView)) {
                    optionView.changeState()
                    grammarTestListener?.playAudio(optionView.choice.audioUrl, null)
                } else
                    optionView.setState(McqOptionState.UNSELECTED)
                grammarTestListener?.toggleSubmitButton(isAnyAnswerSelected())
            }
        }
    }
}