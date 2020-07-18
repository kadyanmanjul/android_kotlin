package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.view.MCQChoicesView
import com.joshtalks.joshskills.ui.assessment.view.Stub

class ChoiceView : FrameLayout {

    private var assessmentType: AssessmentType? = null
    private var assessmentStatus: AssessmentStatus? = null
    private var viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var mcqChoicesStub: Stub<MCQChoicesView>? = null

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
        View.inflate(context, R.layout.choice_view, this)
        mcqChoicesStub = Stub(findViewById(R.id.single_selection_text_stub))
        setUpUI()
    }

    fun bind(
        assessmentType: AssessmentType,
        assessmentStatus: AssessmentStatus,
        viewType: AssessmentQuestionViewType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        this.assessmentType = assessmentType
        this.assessmentStatus = assessmentStatus
        this.viewType = viewType
        this.assessmentQuestion = assessmentQuestion
        setUpUI()
    }

    fun unBind() {
    }

    private fun setUpUI() {
        assessmentQuestion?.let { assessmentQuestion ->
            when (assessmentQuestion.question.choiceType) {

                ChoiceType.SINGLE_SELECTION_TEXT,
                ChoiceType.SINGLE_SELECTION_IMAGE,
                ChoiceType.MULTI_SELECTION_TEXT,
                ChoiceType.MULTI_SELECTION_IMAGE -> {
                    mcqChoicesStub?.run {
                        if (this.resolved().not()) {
                            this.get()
                                ?.bind(
                                    assessmentType!!,
                                    assessmentStatus!!,
                                    viewType,
                                    assessmentQuestion
                                )
                        }
                    }
                    return@let
                }

                ChoiceType.FILL_IN_THE_BLANKS_TEXT -> {
                    null
                }

                ChoiceType.MATCH_TEXT -> {
                    null
                }
            }
        }
    }

}
