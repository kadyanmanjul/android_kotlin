package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.GotoCourseCard
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.course_option_title_layout)
class CourseOptionTitleViewHolder(val title: String, val pos: Int) {

    @com.mindorks.placeholderview.annotations.View(R.id.text_title)
    lateinit var textView: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        textView.text = title
    }

    @Click(R.id.root_view)
    fun onClick() {
        RxBus2.publish(GotoCourseCard(pos))
    }
}
