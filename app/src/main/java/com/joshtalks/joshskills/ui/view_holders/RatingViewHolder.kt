package com.joshtalks.joshskills.ui.view_holders

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
        progressBar.progress = rating.percent
        userRatingPercentage.text = rating.rating.toString().plus("%")
    }
}
