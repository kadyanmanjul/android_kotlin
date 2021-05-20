package com.joshtalks.joshskills.ui.leaderboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LeaderBoardViewPagerAdapter(
    val courseId:String?,
    fragmentActivity: FragmentActivity
) :
    FragmentStateAdapter(fragmentActivity) {


    override fun getItemCount(): Int {
        return 5
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                LeaderBoardFragment.newInstance("TODAY",courseId)
            }
            1 -> {
                LeaderBoardFragment.newInstance("WEEK",courseId)
            }
            2 -> {
                LeaderBoardFragment.newInstance("MONTH",courseId)
            }
            4 -> {
                LeaderBoardFragment.newInstance("BATCH",courseId)
            }
            else -> {
                LeaderBoardFragment.newInstance("LIFETIME",courseId)
            }
        }

    }
}
