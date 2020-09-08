package com.joshtalks.joshskills.ui.newonboarding.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.newonboarding.fragment.CourseSelectionViewPagerFragment
import com.joshtalks.joshskills.ui.newonboarding.fragment.SelectCourseFragment

class CourseSelectionViewPageAdapter(
    fragmentActivity: SelectCourseFragment,
    private val tabNames: MutableList<String>,
    private val courseByMap: Map<String, List<CourseExploreModel>>
) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return courseByMap.size
    }

    override fun createFragment(position: Int): Fragment {
        return CourseSelectionViewPagerFragment.newInstance(
            courseByMap[tabNames[position]] ?: emptyList()
        )
    }
}
