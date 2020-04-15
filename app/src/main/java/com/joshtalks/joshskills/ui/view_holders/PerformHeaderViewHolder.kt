package com.joshtalks.joshskills.ui.view_holders

import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View

@Layout(R.layout.performance_header_layout)
class PerformHeaderViewHolder(var totalPractise: Int, var submittedPractise: Int) {

    @View(R.id.tv_performance_title)
    lateinit var performanceTv: AppCompatTextView

    @Resolve
    fun onViewInflated() {
        performanceTv.text = "$submittedPractise/$totalPractise"
    }
}
