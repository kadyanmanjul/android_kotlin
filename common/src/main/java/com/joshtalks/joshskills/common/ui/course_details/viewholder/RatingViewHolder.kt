package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.repository.server.course_detail.Rating
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


class RatingViewHolder(var rating: Rating) {


    lateinit var userRating: AppCompatTextView


    lateinit var progressBar: ProgressBar


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
