package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomLayout
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.nex3z.flowlayout.FlowLayout

class GrammarChoiceView : RelativeLayout {

    private lateinit var rootView: RelativeLayout
    private lateinit var answerContainer: FrameLayout
    private lateinit var answerFlowLayout: FlowLayout
    private lateinit var optionsContainer: RelativeLayout
    private lateinit var customLayout: CustomLayout
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
        View.inflate(context, R.layout.grammar_choice_view, this)
        rootView = findViewById(R.id.choice_ats_root_view)
        answerContainer = findViewById(R.id.ats_answer_container)
        answerFlowLayout = findViewById(R.id.ats_answer_flow_layout)
        optionsContainer = findViewById(R.id.ats_options_container)
        initOptionsFlowLayout()
    }

    private fun initOptionsFlowLayout() {
        customLayout = CustomLayout(context)
        customLayout.setGravity(Gravity.CENTER)
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        optionsContainer.addView(customLayout, params)
    }

    fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        customLayout.removeAllViews()
        answerFlowLayout.removeAllViews()
        val selectedWords = ArrayList<CustomWord>()
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                val wordView = getWordView(it)
                addChoiceToOptionsLayout(wordView)
                if (it.userSelectedOrder != -1 && it.userSelectedOrder != 100) {
                    selectedWords.add(wordView)
                }
            }

        selectedWords.sortedBy { it.choice.userSelectedOrder }.forEach {
            it.changeViewGroup(customLayout, answerFlowLayout)
        }

    }

    private fun addChoiceToOptionsLayout(word: CustomWord) {
        customLayout.push(word)
    }

    private fun addChoiceToAnswerLayout(word: CustomWord) {
        answerFlowLayout.addView(word)
    }

    private fun getWordView(choice: Choice): CustomWord {
        val customWord = CustomWord(context, choice)
        customWord.setOnTouchListener(TouchListener())
        return customWord
    }

    private fun lockViews() {
        for (i in 0 until answerFlowLayout.childCount) {
            (answerFlowLayout.getChildAt(i) as CustomWord).setEnabled(false)
        }
        for (i in 0 until customLayout.childCount) {
            (customLayout.getChildAt(i) as CustomWord).setEnabled(false)
        }
    }

    private fun unlockViews() {
        for (i in 0 until answerFlowLayout.childCount) {
            (answerFlowLayout.getChildAt(i) as CustomWord).setEnabled(true)
        }
        for (i in 0 until customLayout.childCount) {
            (customLayout.getChildAt(i) as CustomWord).setEnabled(true)
        }
    }

    fun isAnyAnswerSelected() = answerFlowLayout.childCount > 0

    fun isCorrectAnswer(): Boolean {
        lockViews()
        if (isAnyAnswerSelected().not()) {
            unlockViews()
            return false
        } else {
            assessmentQuestion?.question?.isAttempted = true
            assessmentQuestion?.choiceList?.forEach {
                if ((it.correctAnswerOrder == -1 || it.correctAnswerOrder == 100) &&
                    (it.userSelectedOrder != -1 || it.userSelectedOrder != 100)
                ) {
                    return false
                }
                if (it.correctAnswerOrder != -1 &&
                    it.correctAnswerOrder != 100 &&
                    it.userSelectedOrder != it.correctAnswerOrder
                ) {
                    return false
                }
            }
        }
        return true
    }

    private inner class TouchListener : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            if (motionEvent.action == MotionEvent.ACTION_DOWN && !customLayout.isEmpty()) {
                val customWord = view as CustomWord
                customWord.changeViewGroup(customLayout, answerFlowLayout)
                if (isAnyAnswerSelected()){
                    callback?.enableGrammarButton()
                } else {
                    callback?.disableGrammarButton()
                }
                return true
            }
            return false
        }
    }


    fun addCallback(callback: EnableDisableGrammarButtonCallback) {
        this.callback = callback
    }

    interface EnableDisableGrammarButtonCallback {
        fun disableGrammarButton()
        fun enableGrammarButton()
    }

}
