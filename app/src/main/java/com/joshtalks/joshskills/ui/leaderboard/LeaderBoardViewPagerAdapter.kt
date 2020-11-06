package com.joshtalks.joshskills.ui.leaderboard

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
        return LeaderBoardFragment.newInstance(
            hashMap.keys.elementAt(position),
            hashMap.get(hashMap.keys.elementAt(position))
        )
    }

}
