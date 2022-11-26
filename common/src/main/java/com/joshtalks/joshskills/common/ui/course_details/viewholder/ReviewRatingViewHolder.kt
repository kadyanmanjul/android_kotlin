package com.joshtalks.joshskills.common.ui.course_details.viewholder

import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.Reviews
import com.joshtalks.joshskills.common.ui.course_details.extra.ReviewsAdapter
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


class ReviewRatingViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var reviews: Reviews
) : CourseDetailsBaseCell(type, sequenceNumber) {


    lateinit var headerTV: AppCompatTextView


    lateinit var courseRating: AppCompatTextView


    lateinit var ratingRV: PlaceHolderView


    lateinit var reviewRV: RecyclerView


    fun onViewInflated() {
        headerTV.text = reviews.title
        courseRating.text = reviews.value.toString()
        if (ratingRV.viewAdapter == null || ratingRV.viewAdapter.itemCount == 0) {
            ratingRV.builder.setHasFixedSize(true)
                .setLayoutManager(LinearLayoutManager(AppObjectController.joshApplication))
            ratingRV.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(getAppContext(), 4f)))
            reviews.ratingList.sortedByDescending { it.rating }.forEach {
                ratingRV.addView(
                    RatingViewHolder(
                        it
                    )
                )
            }
        }

        if (reviewRV.adapter == null || reviewRV.adapter!!.itemCount == 0) {
            reviewRV.layoutManager =
                LinearLayoutManager(getAppContext(), LinearLayoutManager.VERTICAL, false)
            reviewRV.setHasFixedSize(true)
            reviewRV.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(getAppContext(), 16f)))
            reviewRV.post { reviewRV.adapter = ReviewsAdapter(reviews.reviews) }
        }
    }
}
