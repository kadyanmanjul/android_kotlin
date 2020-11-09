package com.joshtalks.joshskills.ui.leaderboard

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.joshtalks.joshskills.repository.server.LeaderboardResponse

class LeaderBoardViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    var hashMap: HashMap<String, LeaderboardResponse>
) :
    FragmentStateAdapter(fragmentActivity) {


    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                LeaderBoardFragment.newInstance("TODAY", hashMap.get("TODAY"))
            }
            1 -> {
                LeaderBoardFragment.newInstance("WEEK", hashMap.get("WEEK"))
            }
            else -> {
                LeaderBoardFragment.newInstance("MONTH", hashMap.get("MONTH"))
            }
        }

    }
}
