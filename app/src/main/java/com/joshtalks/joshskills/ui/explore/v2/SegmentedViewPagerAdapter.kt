package com.joshtalks.joshskills.ui.explore.v2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.course_recommend.ResponseCourseRecommend
import com.joshtalks.joshskills.ui.explore.CourseListingFragment

class SegmentedViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val recommendCourseLis: List<ResponseCourseRecommend>
) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return recommendCourseLis.size
    }

    override fun createFragment(position: Int): Fragment {
        if (getItemViewType(position) == ExploreCardType.RECOMMENDATION.ordinal) {
            return SegmentedCourseListingFragment.newInstance(recommendCourseLis[position].segmentList)
        } else {
            return CourseListingFragment.newInstance(recommendCourseLis[position].courseList)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return recommendCourseLis[position].exploreCardType.ordinal
    }
}