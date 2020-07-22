package com.joshtalks.joshskills.ui.assessment

import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.test_summay_header_item)
class TestSummaryHeaderViewHolder(
    var assessment: AssessmentWithRelations,
    val context: Context
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
        welcomeMsg.text = context.getString(R.string.hello).plus(User.getInstance().firstName.plus(","))
        totalQuestions.text = context.getString(R.string.total_question).plus(getTotalQuestions())
        totalAttempted.text =context.getString(R.string.total_attempted).plus(getAttemptedQuestions())
    }

    private fun getTotalQuestions() = assessment.questionList.size

    private fun getAttemptedQuestions() =
        assessment.questionList.filter { it.question.isAttempted == true }.size
}
