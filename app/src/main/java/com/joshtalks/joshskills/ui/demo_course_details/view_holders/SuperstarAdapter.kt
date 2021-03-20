package com.joshtalks.joshskills.ui.demo_course_details.view_holders


import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.Feedback

class SuperstarAdapter(
    fm: FragmentManager, private val feedbackList: List<Feedback>,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : FragmentStatePagerAdapter(fm, behavior) {
    override fun getCount(): Int = feedbackList.size

    override fun getItem(position: Int): SuperstarItemFragment =
        SuperstarItemFragment.newInstance((feedbackList[position]))

}
