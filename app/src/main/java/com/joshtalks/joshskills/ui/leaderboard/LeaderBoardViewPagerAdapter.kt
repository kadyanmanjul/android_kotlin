package com.joshtalks.joshskills.ui.leaderboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class LeaderBoardViewPagerAdapter(val courseId: String?, fm: FragmentManager, behavior: Int) :
    FragmentPagerAdapter(fm, behavior) {

    override fun getCount() = 5

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> {
            LeaderBoardFragment.newInstance("TODAY", courseId)
        }
        1 -> {
            LeaderBoardFragment.newInstance("WEEK", courseId)
        }
        2 -> {
            LeaderBoardFragment.newInstance("MONTH", courseId)
        }
        4 -> {
            LeaderBoardFragment.newInstance("BATCH", courseId)
        }
        else -> {
            LeaderBoardFragment.newInstance("LIFETIME", courseId)
        }
    }
}
