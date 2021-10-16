package com.joshtalks.joshskills.ui.leaderboard.search

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.repository.server.LeaderboardType
import java.util.Locale

class LeaderboardSearchPagerAdapter(
    val fm: FragmentManager,
    behavior: Int,
    val map: HashMap<String, LeaderboardResponse>,
    val myBatchTitle: String
) :
    FragmentStatePagerAdapter(fm, behavior) {

    override fun getCount() = 5

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> {
            LeaderboardSearchResultFragment.newInstance(LeaderboardType.TODAY)
        }
        1 -> {
            LeaderboardSearchResultFragment.newInstance(LeaderboardType.WEEK)
        }
        2 -> {
            LeaderboardSearchResultFragment.newInstance(LeaderboardType.MONTH)
        }
        4 -> {
            LeaderboardSearchResultFragment.newInstance(LeaderboardType.BATCH)
        }
        else -> {
            LeaderboardSearchResultFragment.newInstance(LeaderboardType.LIFETIME)
        }
    }

    override fun getPageTitle(position: Int): CharSequence = when (position) {
        0 -> {
            getTitleText(map["TODAY"])
        }
        1 -> {
            getTitleText(map["WEEK"])
        }
        2 -> {
            getTitleText(map["MONTH"])
        }
        4 -> {
            if (map["BATCH"]?.intervalTabText.isNullOrBlank())
                myBatchTitle
            else
                myBatchTitle.plus('\n')
                    .plus(map["BATCH"]?.intervalTabText)
        }
        3 -> {
            getTitleText(map["LIFETIME"])
        }

        else -> {
            ""
        }
    }

    private fun getTitleText(response: LeaderboardResponse?): String =
        if (response?.intervalTabText.isNullOrBlank())
            response?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize() ?: ""
        else
            response?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
                .plus('\n')
                .plus(response?.intervalTabText)


}