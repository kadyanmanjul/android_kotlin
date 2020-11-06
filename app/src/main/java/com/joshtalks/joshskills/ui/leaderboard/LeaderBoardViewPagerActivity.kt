package com.joshtalks.joshskills.ui.leaderboard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import java.util.HashMap

class LeaderBoardViewPagerActivity : AppCompatActivity() {
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_view_pager)
        binding.lifecycleOwner = this
        binding.handler = this
        initViewPager()
        addObserver()
        viewModel.getFullLeaderBoardData(Mentor.getInstance().getId())
    }

    private fun addObserver() {
        viewModel.leaderBoardData.observe(this, Observer {
            binding.viewPager.adapter =
                LeaderBoardViewPagerAdapter(this, it)
            initViewPagerTab()
            setTabText(it)

        })

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            it?.let {
                when (it) {
                    ApiCallStatus.FAILED, ApiCallStatus.SUCCESS -> {
                        binding.progressLayout.visibility = View.GONE
                    }
                    ApiCallStatus.START -> {
                        binding.progressLayout.visibility = View.VISIBLE
                    }
                }
            }
        })
    }

    private fun setTabText(map: HashMap<String, LeaderboardResponse>) {
        val list = map.keys
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (map.get(list.elementAt(position))?.intervalTabText.isNullOrBlank()) {
                tab.text = map.get(list.elementAt(position))?.intervalType
            } else {
                tab.text = map.get(list.elementAt(position))?.intervalType.plus('\n')
                    .plus(map.get(list.elementAt(position))?.intervalTabText)
            }

        }.attach()
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        //binding.viewPager.offscreenPageLimit = 10
    }

    private fun initViewPagerTab() {
        val tabName = resources.getStringArray(R.array.leaderboard_tab)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabName[position]

        }.attach()
    }
}