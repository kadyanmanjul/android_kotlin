package com.joshtalks.joshskills.ui.view_holders

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ExploreCourseEventBus
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.*

@Layout(R.layout.find_more_layout)
class FindMoreViewHolder {

    @Resolve
    fun onResolved() {
    }

    @Click(R.id.parent_layout)
    fun exploreCourses() {
        RxBus2.publish(ExploreCourseEventBus())
    }


}
