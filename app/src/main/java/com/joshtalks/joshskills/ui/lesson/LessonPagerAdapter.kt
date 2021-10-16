package com.joshtalks.joshskills.ui.lesson

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class LessonPagerAdapter(
    fm: FragmentManager,
    behavior: Int,
    val fragmentList: List<Fragment>,
    val lessonIsConvoRoomActive: Boolean
) :
    FragmentPagerAdapter(fm, behavior) {

    override fun getCount() = if (lessonIsConvoRoomActive) 5 else 4

    override fun getItem(position: Int): Fragment = fragmentList.get(position)
}
