package com.joshtalks.joshskills.ui.leaderboard.search

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.LeaderboardType

class LeaderboardSearchPagerAdapter(
    fragmentActivity: FragmentActivity
) :
    FragmentStateAdapter(fragmentActivity) {


    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                LeaderboardSearchResultFragment.newInstance(LeaderboardType.TODAY)
            }
            1 -> {
                LeaderboardSearchResultFragment.newInstance(LeaderboardType.WEEK)
            }
            else -> {
                LeaderboardSearchResultFragment.newInstance(LeaderboardType.MONTH)
            }
        }

    }
}