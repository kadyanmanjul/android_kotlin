package com.joshtalks.joshskills.common.ui.points_history.viewholder

import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.server.points.PointsHistoryTitles
import com.joshtalks.joshskills.common.repository.server.points.SpokenHistory
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View



class SpokenSummaryDescViewHolder(
    var spokenHistory: SpokenHistory,
    var position: Int,
    var totalItems: Int
) {


    lateinit var rootView: ConstraintLayout


    lateinit var title: AppCompatTextView


    lateinit var score: AppCompatTextView


    lateinit var inLesson: AppCompatTextView


    lateinit var divider: android.view.View


    lateinit var dividerTop: android.view.View

    //@ParentPosition
    var mParentPosition: Int = 0

    public val mChildPosition = 0


    fun onViewInflated() {
        if (position == 0) {
            dividerTop.visibility = android.view.View.VISIBLE
        } else {
            dividerTop.visibility = android.view.View.GONE
        }
        if (totalItems == position.plus(1)) {
            val drawable = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rectangle_bottom_rounded_corner_8dp
            )
            rootView.background = drawable
            divider.visibility = android.view.View.INVISIBLE
        } else {
            val drawable = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rect_default_white
            )
            rootView.background = drawable
            divider.visibility = android.view.View.VISIBLE
        }
        //title.text = PointHistoryTitlesArray.getOrNull(spokenHistory.title ?: 0)
        title.text = PointsHistoryTitles.getTitleForIndex(spokenHistory.title ?: 0)
        score.text = "+".plus(spokenHistory.spokenDuration)
        spokenHistory.subTitle?.let {
            inLesson.visibility = android.view.View.VISIBLE
            inLesson.text = it
        }
    }
}