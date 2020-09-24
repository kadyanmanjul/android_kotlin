package com.joshtalks.joshskills.ui.newonboarding.viewholder

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CourseHeadingSelectedEvent
import com.joshtalks.joshskills.repository.server.onboarding.CourseHeading
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.course_heading_item)
class CourseHeadingViewHolder(
    var courseHeading: CourseHeading
) {

    val drawableSelected: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_circle_tick,
            null
        )
    }
    val drawableUnselected: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_circle_boundary,
            null
        )
    }

    @View(R.id.itemView)
    lateinit var itemView: MaterialCardView

    @View(R.id.tick)
    lateinit var tick: ImageView

    @View(R.id.question)
    lateinit var question: TextView

    @Resolve
    fun onViewInflated() {
        initView()
    }

    private fun initView() {
        question.text = courseHeading.name
    }

    @Click(R.id.itemView)
    fun onClick() {
        RxBus2.publish(CourseHeadingSelectedEvent(courseHeading.isSelected, courseHeading.id!!))
        if (courseHeading.isSelected) {
            setUnselectedState()
        } else {
            setSelectedState()
        }
    }

    private fun setSelectedState() {
        courseHeading.isSelected = true
        itemView.setCardBackgroundColor(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.colorPrimary
            )
        )
        tick.setImageDrawable(drawableSelected)
    }

    private fun setUnselectedState() {
        courseHeading.isSelected = false
        itemView.setCardBackgroundColor(
            ContextCompat.getColor(
                AppObjectController.joshApplication,
                R.color.pdf_bg_color
            )
        )
        tick.setImageDrawable(drawableUnselected)
    }
}
