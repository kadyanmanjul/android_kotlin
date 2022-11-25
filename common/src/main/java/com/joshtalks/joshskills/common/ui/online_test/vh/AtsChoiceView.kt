package com.joshtalks.joshskills.common.ui.online_test.vh

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.common.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.common.repository.server.assessment.ChoiceColumn
import com.joshtalks.joshskills.common.ui.online_test.util.AssessmentQuestionViewCallback
import com.joshtalks.joshskills.common.ui.online_test.util.GrammarSubmitButtonListener
import com.nex3z.flowlayout.FlowLayout
import java.util.*

class AtsChoiceView : ConstraintLayout, AssessmentQuestionViewCallback {
    override var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private lateinit var rootView: ConstraintLayout
    private lateinit var answerFlowLayout: FlowLayout
    private lateinit var dummyAnswerFlowLayout: FlowLayout
    private lateinit var optionsFlowLayout: FlowLayout
    private var grammarTestListener: GrammarSubmitButtonListener? = null
    private var answerList: LinkedList<String> = LinkedList()

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
        answerFlowLayout = findViewById(R.id.ats_answer_flow_layout)
        dummyAnswerFlowLayout = findViewById(R.id.dummy_answer_flow_layout)
        optionsFlowLayout = findViewById(R.id.ats_options_layout)
        LayoutTransition().also {
            it.disableTransitionType(LayoutTransition.DISAPPEARING)
            it.disableTransitionType(LayoutTransition.APPEARING)
            it.disableTransitionType(LayoutTransition.CHANGE_APPEARING)
            answerFlowLayout.layoutTransition = it
        }
    }


    override fun setup(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        answerFlowLayout.removeAllViews()
        dummyAnswerFlowLayout.removeAllViews()
        optionsFlowLayout.removeAllViews()
        answerList.clear()
        assessmentQuestion.choiceList
            .sortedBy { it.sortOrder }
            .forEach {
                optionsFlowLayout.addView(getWordView(it))
            }
        addDummyLineView(
            numberOfLines = when {
                optionsFlowLayout.childCount > 13 -> 4
                optionsFlowLayout.childCount > 9 -> 3
                else -> 2
            }
        )
        unlockViews()
    }

    fun addDummyLineView(numberOfLines: Int) {
        dummyAnswerFlowLayout.removeAllViews()
        for (i in 1..numberOfLines) {
            val dummyWordView = AtsOptionView(
                context,
                Choice(0, 0, 0, "Dummy", null, false, 0, 0, ChoiceColumn.LEFT, 0, false, null, null)
            )
            val wordLayoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            wordLayoutParams.gravity = Gravity.CENTER
            wordLayoutParams.setMargins(
                AtsOptionView.mPadding4F,
                AtsOptionView.mPadding4F,
                AtsOptionView.mPadding4F,
                AtsOptionView.mPadding4F
            )
            dummyWordView.layoutParams = wordLayoutParams
            dummyWordView.visibility = INVISIBLE
            dummyAnswerFlowLayout.addView(dummyWordView)
            val line = View(context)
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.dpToPx(1)
            )
            layoutParams.setMargins(
                0,
                0,
                0,
                0
            )
            line.layoutParams = layoutParams
            line.background = ContextCompat.getDrawable(context, R.color.disabled)
            dummyAnswerFlowLayout.addView(line)

        }
    }

    private fun getWordView(choice: Choice): AtsOptionView =
        AtsOptionView(context, choice).apply {
            setOnClickListener(ChoiceClickListener())
        }

    private inner class ChoiceClickListener : OnClickListener {
        override fun onClick(v: View?) {
            if (optionsFlowLayout.isNotEmpty()) {
                val atsOptionView = v as AtsOptionView
                grammarTestListener?.playAudio(
                    atsOptionView.choice.audioUrl,
                    atsOptionView.choice.localAudioUrl
                )
                atsOptionView.choice.text?.let {
                    if (atsOptionView.parent == optionsFlowLayout)
                        answerList.add(it)
                    else
                        answerList.remove(it)
                }
                atsOptionView.changeViewGroup(
                    optionsFlowLayout, answerFlowLayout,
                    atsOptionView.parent == optionsFlowLayout
                )
                grammarTestListener?.toggleSubmitButton(isAnyAnswerSelected())
            }
        }
    }

    override fun lockViews() {
        for (i in 0 until answerFlowLayout.childCount)
            (answerFlowLayout.getChildAt(i) as AtsOptionView).isEnabled = false
        for (i in 0 until optionsFlowLayout.childCount)
            (optionsFlowLayout.getChildAt(i) as AtsOptionView).isEnabled = false
    }

    override fun unlockViews() {
        for (i in 0 until answerFlowLayout.childCount)
            (answerFlowLayout.getChildAt(i) as AtsOptionView).isEnabled = true
        for (i in 0 until optionsFlowLayout.childCount)
            (optionsFlowLayout.getChildAt(i) as AtsOptionView).isEnabled = true
    }

    override fun isAnyAnswerSelected() = answerFlowLayout.childCount > 0

    override fun getAnswerText(): String =
        answerList.joinToString(separator = " ").lowercase(Locale.getDefault())

    override fun addGrammarTestCallback(callback: GrammarSubmitButtonListener) {
        grammarTestListener = callback
    }
}

