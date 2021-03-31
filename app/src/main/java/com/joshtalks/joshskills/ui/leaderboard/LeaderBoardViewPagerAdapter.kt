package com.joshtalks.joshskills.ui.leaderboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LeaderBoardViewPagerAdapter(
    fragmentActivity: FragmentActivity
) :
    FragmentStateAdapter(fragmentActivity) {


    override fun getItemCount(): Int {
        return 5
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                LeaderBoardFragment.newInstance("TODAY")
            }
            1 -> {
                LeaderBoardFragment.newInstance("WEEK")
            }
            2 -> {
                LeaderBoardFragment.newInstance("MONTH")
            }
            3 -> {
                LeaderBoardFragment.newInstance("BATCH")
            }
            else -> {
                LeaderBoardFragment.newInstance("LIFETIME")
            }
        }

    }
}
