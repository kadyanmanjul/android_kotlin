package com.joshtalks.joshskills.ui.assessment

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.TestItemClickedEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.test_summary_item_layout)
class TestItemViewHolder(
    var questionWithRelations: AssessmentQuestionWithRelations,
    var status: AssessmentStatus,
    val context: Context
) {

    @View(R.id.attempt_status)
    lateinit var attempt_status: AppCompatTextView

    @View(R.id.question)
    lateinit var questionText: AppCompatTextView

    @View(R.id.test_button)
    lateinit var testButton: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        initView()
    }

    @SuppressLint("RestrictedApi")
    private fun initView() {
        if (status == AssessmentStatus.STARTED || status == AssessmentStatus.NOT_STARTED) {
            if (isQuestionAttempted(questionWithRelations)) {
                attempt_status.text = context.getString(R.string.attempted)
            } else {
                attempt_status.text = context.getString(R.string.not_attempted)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(context.resources, R.color.dark_grey, null)
            }
            testButton.text = context.getString(R.string.edit_answer)
        } else {

            when (questionWithRelations.question.status) {
                QuestionStatus.CORRECT -> {
                    attempt_status.text = context.getString(R.string.right)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(context.resources, R.color.green, null)
                }
                QuestionStatus.WRONG -> {
                    attempt_status.text = context.getString(R.string.wrong)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(context.resources, R.color.red, null)
                }
                QuestionStatus.SKIPPED, QuestionStatus.NONE -> {
                    attempt_status.text = context.getString(R.string.not_attempted)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(
                            context.resources,
                            R.color.dark_grey,
                            null
                        )
                }

            }
            testButton.text = context.getString(R.string.view_answer)
        }
        questionText.text = questionWithRelations.question.text
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

    @Click(R.id.test_button)
    fun onClick() {
        RxBus2.publish(TestItemClickedEventBus(questionWithRelations.question.sortOrder))
    }
}
