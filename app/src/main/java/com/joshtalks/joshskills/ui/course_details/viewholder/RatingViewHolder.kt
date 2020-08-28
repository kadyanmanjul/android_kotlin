package com.joshtalks.joshskills.ui.course_details.viewholder

import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.course_detail.Rating
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.rating_item_layout)
class RatingViewHolder(var rating: Rating) {

    @View(R.id.user_rating)
    lateinit var userRating: AppCompatTextView

    @View(R.id.progress_bar)
    lateinit var progressBar: ProgressBar

    @View(R.id.user_rating_percentage)
    lateinit var userRatingPercentage: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        userRating.text = rating.rating.toString()
        userRatingPercentage.text = rating.percent.toString().plus("%")
        progressBarAnimate()
    }

    private fun progressBarAnimate() {
        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(progressBar, "progress", rating.percent)
        animation.duration = 750
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }
}
