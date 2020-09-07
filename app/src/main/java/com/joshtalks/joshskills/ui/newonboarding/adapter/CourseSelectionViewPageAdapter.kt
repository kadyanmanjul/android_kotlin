package com.joshtalks.joshskills.ui.newonboarding.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.newonboarding.fragment.CourseSelectionViewPagerFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectCourseFragment

class CourseSelectionViewPageAdapter(
    fragmentActivity: SelectCourseFragment, private val courseByMap: Map<Int, List<CourseExploreModel>>
) :
    FragmentStateAdapter(fragmentActivity) {
    private val mKeys: IntArray = courseByMap.keys.toIntArray()

    override fun getItemCount(): Int {
        return courseByMap.size
    }

    override fun createFragment(position: Int): Fragment {
        return CourseSelectionViewPagerFragment.newInstance(getItem(position))
    }

    private fun getItem(position: Int): List<CourseExploreModel> {
        return courseByMap[mKeys[position]] ?: emptyList()
    }
}
