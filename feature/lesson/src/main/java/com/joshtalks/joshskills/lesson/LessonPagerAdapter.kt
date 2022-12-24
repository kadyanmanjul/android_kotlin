package com.joshtalks.joshskills.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val fragmentList:List<Fragment>,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = fragmentList.size
    override fun createFragment(position: Int) = fragmentList.get(position)
}
