package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.repository.server.course_detail.StudentFeedback
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.layout_student_feedback_viewholder)
class StudentFeedbackViewHolder(
    private var studentFeedback: StudentFeedback,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell() {
    @com.mindorks.placeholderview.annotations.View(R.id.story_recycler_view)
    lateinit var item: PlaceHolderView
    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @Resolve
    fun onResolved() {
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        item.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        item.addItemDecoration(
            LayoutMarginDecoration(
                com.vanniktech.emoji.Utils.dpToPx(
                    context,
                    2f
                )
            )
        )
        item.itemAnimator = null
        studentFeedback.feedbacks.forEach {
            item.addView(StudentFeedbackCard(it))

        }
    }
}