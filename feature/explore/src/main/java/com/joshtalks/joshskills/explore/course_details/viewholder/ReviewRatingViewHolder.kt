package com.joshtalks.joshskills.explore.course_details.viewholder

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.course_details.adapters.ReviewsAdapter
import com.joshtalks.joshskills.explore.course_details.models.Rating
import com.joshtalks.joshskills.explore.course_details.models.Reviews
import com.joshtalks.joshskills.explore.databinding.ReviewAndRatingLayoutBinding

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

        item.ratingRv.removeAllViews()
        data.ratingList.sortedByDescending { it.rating }.forEach {
            val view = getRatingView(it)
            if (view != null) {
                item.ratingRv.addView(view)
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

    private fun getRatingView(rating: Rating): View? {
        val ratingInflate = getAppContext()?.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val ratingCard = ratingInflate.inflate(R.layout.rating_item_layout, null, true)

        ratingCard?.findViewById<AppCompatTextView>(R.id.user_rating)?.text = rating.rating.toString()
        ratingCard?.findViewById<AppCompatTextView>(R.id.user_rating_percentage)?.text = rating.percent.toString().plus("%")

        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(ratingCard?.findViewById<ProgressBar>(R.id.progress_bar), "progress", rating.percent)
        animation.duration = 750
        animation.interpolator = DecelerateInterpolator()
        animation.start()

        return ratingCard
    }
}
