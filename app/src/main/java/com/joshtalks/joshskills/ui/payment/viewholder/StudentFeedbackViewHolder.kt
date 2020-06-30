package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.StudentFeedback
import com.joshtalks.joshskills.ui.view_holders.CourseDetailsBaseCell
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve

@Layout(R.layout.layout_student_feedback_viewholder)
class StudentFeedbackViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var studentFeedback: StudentFeedback,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.story_recycler_view)
    lateinit var item: PlaceHolderView

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView
    private val linearLayoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    @Resolve
    fun onResolved() {
        title.text = studentFeedback.title
        if (item.adapter == null || item.adapter!!.itemCount == 0) {
            linearLayoutManager.isSmoothScrollbarEnabled = true
            item.itemAnimator = null
            item.builder.setHasFixedSize(true).setLayoutManager(linearLayoutManager)
            studentFeedback.feedbacks.forEach {
                item.addView(StudentFeedbackCard(it))
            }
        }

        /*if (item.itemDecorationCount < 1) {
            val cardWidthPixels = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            item.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    context,
                    cardWidthPixels,
                    cardHintPercent
                )
            )
        }*/

    }
}
