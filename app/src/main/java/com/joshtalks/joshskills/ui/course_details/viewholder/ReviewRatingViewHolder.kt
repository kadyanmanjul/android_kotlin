package com.joshtalks.joshskills.ui.course_details.viewholder

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.ReviewAndRatingLayoutBinding
import com.joshtalks.joshskills.repository.server.course_detail.Reviews
import com.joshtalks.joshskills.ui.course_details.extra.ReviewsAdapter

class ReviewRatingViewHolder(
    val item: ReviewAndRatingLayoutBinding
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            Reviews::class.java
        )
        item.header.text = data.title
        item.courseRating.text = data.value.toString()
        if (item.ratingRv.viewAdapter == null || item.ratingRv.viewAdapter.itemCount == 0) {
            item.ratingRv.builder.setHasFixedSize(true)
                .setLayoutManager(LinearLayoutManager(AppObjectController.joshApplication))
            item.ratingRv.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(getAppContext(), 4f)))
            data.ratingList.sortedByDescending { it.rating }.forEach {
                item.ratingRv.addView(
                    RatingViewHolder(it)
                )
            }
        }

        if (item.reviewRv.adapter == null || item.reviewRv.adapter!!.itemCount == 0) {
            item.reviewRv.layoutManager =
                LinearLayoutManager(getAppContext(), LinearLayoutManager.VERTICAL, false)
            item.reviewRv.setHasFixedSize(true)
            item.reviewRv.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(getAppContext(), 16f)))
            item.reviewRv.post { item.reviewRv.adapter = ReviewsAdapter(data.reviews) }
        }
    }
}
