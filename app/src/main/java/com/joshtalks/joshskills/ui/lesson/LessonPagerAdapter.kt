package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LessonPagerAdapter(
    fragmentActivity: FragmentActivity,
    behavior: Int,
    val fragmentList: List<Fragment>,
    val lessonIsConvoRoomActive: Boolean
) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = if (lessonIsConvoRoomActive) 5 else 4
    override fun createFragment(position: Int) = fragmentList.get(position)
}
