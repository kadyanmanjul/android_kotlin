package com.joshtalks.joshskills.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenPreviousLeaderboard
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.ui.leaderboard.search.LeaderBoardSearchActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.base_toolbar.*
import java.util.*

class LeaderBoardViewPagerActivity : WebRtcMiddlewareActivity() {
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }
    var mapOfVisitedPage = HashMap<Int, Int>()
    private var compositeDisposable = CompositeDisposable()
    private var tabPosition = 0

    val searchActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.getFullLeaderBoardData(Mentor.getInstance().getId())
            }
        }


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
        with(iv_earn) {
            visibility = View.GONE
            setImageDrawable(
                ContextCompat.getDrawable(
                    this@LeaderBoardViewPagerActivity,
                    R.drawable.ic_search
                )
            )
            setOnClickListener { openSearchActivity() }
        }
        text_message_title.text = getString(R.string.leaderboard)
    }

    private fun openSearchActivity() {
        searchActivityResult.launch(
            LeaderBoardSearchActivity.getSearchActivityIntent(
                this,
                viewModel.leaderBoardData.value
            )
        )
    }

    private fun addObserver() {
        viewModel.leaderBoardData.observe(this, Observer {
            mapOfVisitedPage.put(0, 0)
            mapOfVisitedPage.put(1, 0)
            mapOfVisitedPage.put(2, 0)

            setTabText(it)

            binding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tabPosition=position
                    mapOfVisitedPage.put(position, mapOfVisitedPage.get(position)?.plus(1) ?: 1)
                    viewModel.engageLeaderBoardimpression(mapOfVisitedPage, position)
                }
            })
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
            when (position) {
                0 -> {
                    list = "TODAY"
                }
                1 -> {
                    list = "WEEK"
                }
                2 -> {
                    list = "MONTH"
                }
            }
            if (map.get(list)?.intervalTabText.isNullOrBlank()) {
                tab.text =
                    map.get(list)?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
            } else {
                tab.text =
                    map.get(list)?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
                        .plus('\n')
                        .plus(map.get(list)?.intervalTabText)
            }

        }.attach()
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.adapter =
            LeaderBoardViewPagerAdapter(this)
        //binding.viewPager.offscreenPageLimit = 10
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }


    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenPreviousLeaderboard::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    var type= EMPTY
                    when (tabPosition) {
                        0 -> {
                            type = "TODAY"
                        }
                        1 -> {
                            type = "WEEK"
                        }
                        2 -> {
                            type = "MONTH"
                        }
                    }
                    if (type.isNotBlank()) {
                        openPreviousLeaderBoard(type)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun openPreviousLeaderBoard(intervalType: String) {
        PreviousLeaderboardActivity.startPreviousLeaderboardActivity(
            this,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            intervalType
        )
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
