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
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import kotlinx.android.synthetic.main.base_toolbar.iv_back
import kotlinx.android.synthetic.main.base_toolbar.iv_help
import kotlinx.android.synthetic.main.base_toolbar.text_message_title
import java.util.HashMap

class LeaderBoardViewPagerActivity : BaseActivity() {
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_view_pager)
        binding.lifecycleOwner = this
        binding.handler = this
        initToolbar()
        initViewPager()
        addObserver()
        viewModel.getFullLeaderBoardData(Mentor.getInstance().getId())
        showProgressBar()
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        with(iv_help) {
            visibility = View.VISIBLE
            setOnClickListener {
                openHelpActivity()
            }
        }
        text_message_title.text = getString(R.string.leaderboard)
    }

    private fun addObserver() {
        viewModel.leaderBoardData.observe(this, Observer {
            binding.viewPager.adapter =
                LeaderBoardViewPagerAdapter(this, it)
            setTabText(it)

        })

        viewModel.apiCallStatusLiveData.observe(this, Observer {
            it?.let {
                when (it) {
                    ApiCallStatus.FAILED, ApiCallStatus.SUCCESS -> {
                        hideProgressBar()
                    }
                    ApiCallStatus.START -> {
                        showProgressBar()
                    }
                }
            }
        })
    }

    private fun setTabText(map: HashMap<String, LeaderboardResponse>) {
        var list = EMPTY
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when(position){
                0->{
                    list = "TODAY"
                }
                1->{
                    list = "WEEK"
                }
                2->{
                    list = "MONTH"
                }
            }
            if (map.get(list)?.intervalTabText.isNullOrBlank()) {
                tab.text = map.get(list)?.intervalType
            } else {
                tab.text = map.get(list)?.intervalType.plus('\n')
                    .plus(map.get(list)?.intervalTabText)
            }

        }.attach()
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        //binding.viewPager.offscreenPageLimit = 10
    }

}
