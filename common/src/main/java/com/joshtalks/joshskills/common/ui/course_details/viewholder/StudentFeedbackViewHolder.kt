package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.common.repository.server.course_detail.StudentFeedback
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


class StudentFeedbackViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var studentFeedback: StudentFeedback,
    private val context: Context = AppObjectController.joshApplication,
    private val testId: Int,
    private val coursePrice: String,
    private val courseName: String
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var item: PlaceHolderView

    
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
            if (item.itemDecorationCount < 1) {
                val cardWidthPixels = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()
                val cardHintPercent = 0.01f
                item.addItemDecoration(
                    RecyclerViewCarouselItemDecorator(
                        context,
                        cardWidthPixels,
                        cardHintPercent
                    )
                )
            }
            studentFeedback.feedbacks.forEach {
                item.addView(
                    StudentFeedbackCard(
                        it,
                        context,
                        testId,
                        coursePrice,
                        courseName
                    )
                )
            }

        }
    }
}
