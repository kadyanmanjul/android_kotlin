package com.joshtalks.joshskills.ui.points_history.viewholder

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.textColorSet
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.expand.Collapse
import com.mindorks.placeholderview.annotations.expand.Expand
import com.mindorks.placeholderview.annotations.expand.Parent
import com.mindorks.placeholderview.annotations.expand.SingleTop
import com.mindorks.placeholderview.annotations.expand.Toggle

@Parent
@SingleTop
@Layout(R.layout.layout_point_summary_parent_item)
class PointsSummaryTitleViewHolder(var date: String, var point: Int, var index: Int) {

    @Toggle(R.id.root_view)
    @View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @View(R.id.name)
    lateinit var name: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @View(R.id.expand_unexpand_view)
    lateinit var toggleView: AppCompatImageView

    var isExpanded=false

    private val drawableDown: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_down_32,
            null
        )
    }
    val drawableUp: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_blue_arrow_up_32,
            null
        )
    }

    val drawable = ContextCompat.getDrawable(
        AppObjectController.joshApplication,
        R.drawable.rectangle_with_blue_bound_stroke_corner_8dp
    )
    val drawableSqaure = ContextCompat.getDrawable(
        AppObjectController.joshApplication,
        R.drawable.rectangle_top_rounded_corner_8dp
    )

    @Resolve
    fun onResolved() {
        name.text = date
        if (index == 0) {
            name.textColorSet(R.color.colorPrimary)
            onExpand()
        } else {
            name.textColorSet(R.color.black)
            onCollapse()
        }
        rootView.background = drawable
        score.text = point.toString()
    }

    @Expand
    fun onExpand() {
        rootView.background = drawableSqaure
        toggleView.setImageDrawable(drawableUp)
        name.textColorSet(R.color.colorPrimary)
    }

    @Collapse
    fun onCollapse() {
        rootView.background = drawable
        name.textColorSet(R.color.black)
        toggleView.setImageDrawable(drawableDown)
    }
}
