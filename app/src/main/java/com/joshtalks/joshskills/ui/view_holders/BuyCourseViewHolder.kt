package com.joshtalks.joshskills.ui.view_holders

import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.BuyCourseEventBus
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


@Layout(R.layout.buy_course_layout)
class BuyCourseViewHolder(private val courseId: String) {

    @View(R.id.find_more)
    lateinit var materialTextView: MaterialTextView

    @Resolve
    fun onResolved() {
        materialTextView.text = AppObjectController.joshApplication.getString(R.string.start_course)
    }

    @Click(R.id.find_more)
    fun buyCourse() {
        RxBus2.publish(BuyCourseEventBus(courseId))
    }
}
