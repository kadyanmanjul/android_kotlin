package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType
import com.joshtalks.joshskills.ui.assessment.view.FillInTheBlankChoiceView
import com.joshtalks.joshskills.ui.assessment.view.MCQChoicesView
import com.joshtalks.joshskills.ui.assessment.view.MatchTheFollowingChoiceView
import com.joshtalks.joshskills.ui.assessment.view.Stub
import timber.log.Timber

class ChoiceView : FrameLayout {

    private var assessment: Assessment? = null
    private var viewType = AssessmentQuestionViewType.CORRECT_ANSWER_VIEW
    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private var fillInTheBlankChoiceStub: Stub<FillInTheBlankChoiceView>? = null
    private var matchTheFollowingChoiceStub: Stub<MatchTheFollowingChoiceView>? = null
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
        fillInTheBlankChoiceStub = Stub(findViewById(R.id.fill_in_the_blank_stub))
        matchTheFollowingChoiceStub = Stub(findViewById(R.id.match_the_following_stub))
    }

    fun bind(
        assessment: Assessment,
        viewType: AssessmentQuestionViewType,
        assessmentQuestion: AssessmentQuestionWithRelations
    ) {
        this.assessment = assessment
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
                                    assessment!!,
                                    viewType,
                                    assessmentQuestion
                                )
                        }
                    }
                    return@let
                }

                ChoiceType.FILL_IN_THE_BLANKS_TEXT -> {
                    fillInTheBlankChoiceStub?.run {
                        if (this.resolved().not()) {
                            this.get()
                                ?.bind(
                                    assessment!!,
                                    viewType,
                                    assessmentQuestion
                                )
                        }
                    }
                    return@let
                }

                ChoiceType.MATCH_TEXT -> {
                    matchTheFollowingChoiceStub?.run {
                        if (this.resolved().not()) {
                            this.get()
                                ?.bind(
                                    assessment!!,
                                    viewType,
                                    assessmentQuestion
                                )
                        }
                    }
                    return@let
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.tag("onDetachedFromWindow").e("AudioPlayer")
    }
}
