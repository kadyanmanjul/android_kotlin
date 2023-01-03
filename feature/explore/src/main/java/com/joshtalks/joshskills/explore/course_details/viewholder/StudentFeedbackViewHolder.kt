package com.joshtalks.joshskills.explore.course_details.viewholder

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.explore.course_details.adapters.StudentFeedbackAdapter
import com.joshtalks.joshskills.explore.course_details.models.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.explore.course_details.models.StudentFeedback
import com.joshtalks.joshskills.explore.databinding.LayoutStudentFeedbackViewholderBinding

class StudentFeedbackViewHolder(
    val item: LayoutStudentFeedbackViewholderBinding,
    val testId: Int
) : DetailsBaseViewHolder(item) {

    private val linearLayoutManager =
        LinearLayoutManager(getAppContext(), LinearLayoutManager.HORIZONTAL, false)

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            StudentFeedback::class.java
        )
        item.title.text = data.title
        if (item.storyRecyclerView.adapter == null || item.storyRecyclerView.adapter!!.itemCount == 0) {
            linearLayoutManager.isSmoothScrollbarEnabled = true
            item.storyRecyclerView.itemAnimator = null
            item.storyRecyclerView.setHasFixedSize(true)
            item.storyRecyclerView.layoutManager = linearLayoutManager
            if (item.storyRecyclerView.itemDecorationCount < 1) {
                val cardWidthPixels = (getAppContext().resources.displayMetrics.widthPixels * 0.90f).toInt()
                val cardHintPercent = 0.01f
                item.storyRecyclerView.addItemDecoration(
                    RecyclerViewCarouselItemDecorator(
                        getAppContext(),
                        cardWidthPixels,
                        cardHintPercent
                    )
                )
            }
            item.storyRecyclerView.adapter = StudentFeedbackAdapter(data.feedbacks)
        }
    }
}
