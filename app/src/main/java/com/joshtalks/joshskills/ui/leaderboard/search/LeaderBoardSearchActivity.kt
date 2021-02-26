package com.joshtalks.joshskills.ui.leaderboard.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityLeaderboardSearchBinding
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.Period
import java.time.temporal.TemporalAdjusters.next


class LeaderBoardSearchActivity : BaseActivity() {
    lateinit var binding: ActivityLeaderboardSearchBinding
    private val searchViewModel by lazy { ViewModelProvider(this).get(LeaderBoardSearchViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_search)
        binding.lifecycleOwner = this
        binding.handler = this

        initViewPager()

        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchViewModel.performSearch(s.toString())

                if (s.toString().isNotEmpty())
                    showViewpager()
                else
                    hideViewpager()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

        })
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.adapter =
            LeaderboardSearchPagerAdapter(this)

        hideViewpager()
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "TODAY"
                }
                1 -> {
                    val now = LocalDate.now()

                    val weekEnd = LocalDate.now().with(next(SUNDAY))

                    tab.text =
                        "Week"
                            .plus('\n')
                            .plus("(${Period.between(now, weekEnd).days + 1} days left)")
                }
                2 -> {
                    val now = LocalDate.now()
                    val monthEnd = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1)
                    tab.text =
                        "Month"
                            .plus('\n')
                            .plus("(${Period.between(now, monthEnd).days + 1} days left)")
                }
            }

        }.attach()


    }

    fun hideViewpager() {
        binding.viewPager.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE
        binding.rank.visibility = View.GONE
        binding.points.visibility = View.GONE
        binding.name.visibility = View.GONE
        binding.searchLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
        binding.backIv.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.clearIv.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.searchBg.background = ContextCompat.getDrawable(this, R.drawable.grey_rounded_bg)
    }

    fun showViewpager() {
        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.rank.visibility = View.VISIBLE
        binding.points.visibility = View.VISIBLE
        binding.name.visibility = View.VISIBLE
        binding.searchLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        binding.backIv.setColorFilter(ContextCompat.getColor(this, R.color.white))
        binding.clearIv.setColorFilter(ContextCompat.getColor(this, R.color.white))
        binding.searchBg.background =
            ContextCompat.getDrawable(this, R.drawable.primary_dark_rounded_bg)
    }

    fun clearSearchText() {
        binding.searchView.setText(EMPTY)
    }

    companion object {
        fun getSearchActivityIntent(context: Context): Intent {
            return Intent(context, LeaderBoardSearchActivity::class.java)
        }
    }

}