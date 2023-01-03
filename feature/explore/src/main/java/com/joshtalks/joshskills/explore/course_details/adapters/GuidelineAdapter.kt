package com.joshtalks.joshskills.explore.course_details.adapters

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.explore.course_details.models.Guideline

class GuidelineAdapter(
    fm: FragmentManager,
    private val guidelines: List<Guideline>,
    behavior: Int = BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) : FragmentStatePagerAdapter(fm, behavior) {
    override fun getCount(): Int = guidelines.size

    override fun getItem(position: Int): GuidelineFragment =
        GuidelineFragment.newInstance((guidelines[position]))

    override fun getPageTitle(position: Int): CharSequence? {
        return guidelines[position].category
    }
}
