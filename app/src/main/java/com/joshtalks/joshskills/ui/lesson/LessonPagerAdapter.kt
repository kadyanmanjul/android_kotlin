package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class LessonPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    val fragmentList:List<Fragment>,
    val lessonIsConvoRoomActive: Boolean
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = if (lessonIsConvoRoomActive) 5 else 4
    override fun createFragment(position: Int) = fragmentList.get(position)
}
