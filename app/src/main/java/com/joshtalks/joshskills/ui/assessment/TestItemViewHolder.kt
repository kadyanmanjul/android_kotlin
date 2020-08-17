package com.joshtalks.joshskills.ui.assessment

import android.annotation.SuppressLint
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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
    var status: AssessmentStatus
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
                attempt_status.text =
                    AppObjectController.joshApplication.getString(R.string.attempted)
            } else {
                attempt_status.text =
                    AppObjectController.joshApplication.getString(R.string.not_attempted)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(
                        AppObjectController.joshApplication.resources,
                        R.color.dark_grey,
                        null
                    )
            }
            testButton.text = AppObjectController.joshApplication.getString(R.string.edit_answer)
        } else {

            when (questionWithRelations.question.status) {
                QuestionStatus.CORRECT -> {
                    attempt_status.text =
                        AppObjectController.joshApplication.getString(R.string.right)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(
                            AppObjectController.joshApplication.resources,
                            R.color.green,
                            null
                        )
                }
                QuestionStatus.WRONG -> {
                    attempt_status.text =
                        AppObjectController.joshApplication.getString(R.string.wrong)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(
                            AppObjectController.joshApplication.resources,
                            R.color.red,
                            null
                        )
                }
                QuestionStatus.SKIPPED, QuestionStatus.NONE -> {
                    attempt_status.text =
                        AppObjectController.joshApplication.getString(R.string.not_attempted)
                    attempt_status.supportBackgroundTintList =
                        ResourcesCompat.getColorStateList(
                            AppObjectController.joshApplication.resources,
                            R.color.dark_grey,
                            null
                        )
                }

            }
            testButton.text = AppObjectController.joshApplication.getString(R.string.view_answer)
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

            ChoiceType.FILL_IN_THE_BLANKS_TEXT, ChoiceType.MATCH_TEXT -> {
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
        }
        return false
    }

    @Click(R.id.test_button)
    fun onClick() {
        if (status == AssessmentStatus.STARTED || status == AssessmentStatus.NOT_STARTED) {
            logEditAnswerClickedEvent()
        } else {
            logViewAnswerClickedEvent()
        }
        RxBus2.publish(
            TestItemClickedEventBus(questionWithRelations.question.remoteId)
        )
    }

    private fun logEditAnswerClickedEvent() {
        AppAnalytics.create(AnalyticsEvent.EDIT_ANSWER_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.QUESTION_ID.NAME, questionWithRelations.question.remoteId)
            .addParam(
                AnalyticsEvent.ASSESSMENT_ID.NAME,
                questionWithRelations.question.assessmentId
            )
            .push()
    }

    private fun logViewAnswerClickedEvent() {
        AppAnalytics.create(AnalyticsEvent.VIEW_ANSWER_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.QUESTION_ID.NAME, questionWithRelations.question.remoteId)
            .addParam(
                AnalyticsEvent.ASSESSMENT_ID.NAME,
                questionWithRelations.question.assessmentId
            )
            .push()
    }
}
