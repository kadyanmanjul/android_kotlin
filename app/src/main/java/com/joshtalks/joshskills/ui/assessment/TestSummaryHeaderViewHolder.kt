package com.joshtalks.joshskills.ui.assessment

import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.test_summay_header_item)
class TestSummaryHeaderViewHolder(
    var assessment: AssessmentWithRelations
) {

    @View(R.id.welcome_msg)
    lateinit var welcomeMsg: AppCompatTextView

    @View(R.id.total_questions)
    lateinit var totalQuestions: AppCompatTextView

    @View(R.id.total_attempted)
    lateinit var totalAttempted: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        initView()
    }

    private fun initView() {
        welcomeMsg.text =
            AppObjectController.joshApplication.getString(R.string.hello)
                .plus(User.getInstance().firstName.plus(","))
        totalQuestions.text = AppObjectController.joshApplication.getString(R.string.total_question)
            .plus(getTotalQuestions())
        totalAttempted.text =
            AppObjectController.joshApplication.getString(R.string.total_attempted)
                .plus(getAttemptedQuestions())
    }

    private fun getTotalQuestions() = assessment.questionList.size

    private fun getAttemptedQuestions(): Int {
        var attempted = 0
        assessment.questionList.forEach { question ->
            if (isQuestionAttempted(question)) {
                attempted++
            }
        }
        return attempted
    }

    private fun isQuestionAttempted(assessmentQuestion: AssessmentQuestionWithRelations): Boolean {
        when (assessmentQuestion.question.choiceType) {

            ChoiceType.SINGLE_SELECTION_TEXT,
            ChoiceType.SINGLE_SELECTION_IMAGE,
            ChoiceType.MULTI_SELECTION_TEXT,
            ChoiceType.MULTI_SELECTION_IMAGE -> {
                assessmentQuestion.choiceList.forEach {
                    if (it.isSelectedByUser) {
                        return true
                    }
                }
            }

            ChoiceType.FILL_IN_THE_BLANKS_TEXT -> {
                var numberOfChoicesSelected = 0
                assessmentQuestion.choiceList.forEach {
                    if (it.isSelectedByUser) {
                        numberOfChoicesSelected++
                    }
                }
                if (numberOfChoicesSelected == assessmentQuestion.choiceList.size) {
                    return true
                }
            }

            else ->
                return false
        }
        return false
    }
}
