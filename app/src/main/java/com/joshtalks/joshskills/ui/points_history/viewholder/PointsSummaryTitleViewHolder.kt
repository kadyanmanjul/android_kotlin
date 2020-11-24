package com.joshtalks.joshskills.ui.points_history.viewholder

import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
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
class PointsSummaryTitleViewHolder(var date: String, var point: Double) {

    @Toggle(R.id.root_view)
    @View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @View(R.id.name)
    lateinit var name: AppCompatTextView

    @View(R.id.score)
    lateinit var score: AppCompatTextView

    @View(R.id.expand_unexpand_view)
    lateinit var toggleView: AppCompatImageView

    private val drawableDown: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_down_24,
            null
        )
    }
    val drawableUp: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_baseline_keyboard_arrow_up_24,
            null
        )
    }

    @Resolve
    fun onResolved() {
        name.text=date
        score.text=point.toString()
    }

    @Expand
    fun onExpand() {
        toggleView.setImageDrawable(drawableUp)
    }

    @Collapse
    fun onCollapse() {
        toggleView.setImageDrawable(drawableDown)
    }
}
