package com.joshtalks.joshskills.ui.points_history.viewholder

import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.points.PointsHistory
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.ChildPosition
import com.mindorks.placeholderview.annotations.expand.ParentPosition


@Layout(R.layout.layout_point_summary_child_item)
class PointsSummaryDescViewHolder(var pointsHistory: PointsHistory,var position:Int,var totalItems:Int) {

    @View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @View(R.id.in_lesson)
    lateinit var inLesson: AppCompatTextView

    @View(R.id.divider)
    lateinit var divider: android.view.View
    @View(R.id.divider_top)
    lateinit var dividerTop: android.view.View

    //@ParentPosition
    var mParentPosition :Int=0

    public val mChildPosition = 0

    @Resolve
    fun onViewInflated() {
        if(position==0){
            dividerTop.visibility=android.view.View.VISIBLE
        } else {
            dividerTop.visibility=android.view.View.GONE
        }
        if (totalItems==position.plus(1)){
            val drawable = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rectangle_bottom_rounded_corner_8dp
            )
            rootView.background=drawable
            divider.visibility=android.view.View.INVISIBLE
        } else{
            val drawable = ContextCompat.getDrawable(
                AppObjectController.joshApplication,
                R.drawable.rect_default_white
            )
            rootView.background=drawable
            divider.visibility=android.view.View.VISIBLE
        }
        title.text = pointsHistory.title
        score.text = "+".plus(pointsHistory.points)
        pointsHistory.subTitle?.let {
            inLesson.visibility=android.view.View.VISIBLE
            inLesson.text = it
        }
    }
}
