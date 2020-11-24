package com.joshtalks.joshskills.ui.points_history.viewholder

import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.points.PointsHistory
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.ChildPosition
import com.mindorks.placeholderview.annotations.expand.ParentPosition


@Layout(R.layout.layout_point_summary_child_item)
class PointsSummaryDescViewHolder(var pointsHistory: PointsHistory) {

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @View(R.id.in_lesson)
    lateinit var inLesson: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        title.text = pointsHistory.title
        score.text = "+".plus(pointsHistory.points)
        inLesson.text = pointsHistory.subTitle

    }
}
