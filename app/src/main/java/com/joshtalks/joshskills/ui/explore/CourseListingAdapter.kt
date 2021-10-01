package com.joshtalks.joshskills.ui.explore

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class PractiseViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val courseByMap: Map<Int, List<CourseExploreModel>>,
    private val isClickable: Boolean
) :
    FragmentStateAdapter(fragmentActivity) {
    private val mKeys: IntArray = courseByMap.keys.toIntArray()

    override fun getItemCount(): Int {
        return courseByMap.size
    }

    override fun createFragment(position: Int): Fragment {
        return CourseListingFragment.newInstance(getItem(position),isClickable)
    }

    private fun getItem(position: Int): List<CourseExploreModel> {
        return courseByMap[mKeys[position]] ?: emptyList()
    }
}
