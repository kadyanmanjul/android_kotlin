package com.joshtalks.joshskills.ui.assessment

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.TestItemClickedEventBus
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.QuestionStatus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.test_summary_item_layout)
class TestItemViewHolder(
    var question: AssessmentQuestion,
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
            if (question.isAttempted) {
                attempt_status.text = context.getString(R.string.attempted)
            } else {
                attempt_status.text = context.getString(R.string.not_attempted)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(context.resources, R.color.dark_grey, null)
            }
            testButton.text = context.getString(R.string.edit_answer)
        } else {
            if (!question.isAttempted) {
                attempt_status.text = context.getString(R.string.not_attempted)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(context.resources, R.color.dark_grey, null)
            } else if (question.status == QuestionStatus.CORRECT) {
                attempt_status.text = context.getString(R.string.right)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(context.resources, R.color.green, null)

            } else {
                attempt_status.text = context.getString(R.string.wrong)
                attempt_status.supportBackgroundTintList =
                    ResourcesCompat.getColorStateList(context.resources, R.color.red, null)
            }
            testButton.text = context.getString(R.string.view_answer)
        }
        questionText.text = question.text
    }

    @Click(R.id.test_button)
    fun onClick() {
        RxBus2.publish(TestItemClickedEventBus(question.remoteId))
    }
}
