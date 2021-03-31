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
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityLeaderboardSearchBinding
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearch
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import java.util.ArrayList
import java.util.Locale
import com.joshtalks.joshskills.track.CONVERSATION_ID


class LeaderBoardSearchActivity : BaseActivity() {
    private lateinit var adapter: RecentSearchListAdapter
    private val itemList: MutableList<RecentSearch> = ArrayList()
    lateinit var binding: ActivityLeaderboardSearchBinding
    private val searchViewModel by lazy { ViewModelProvider(this).get(LeaderBoardSearchViewModel::class.java) }
    private var map: HashMap<String, LeaderboardResponse> = hashMapOf()
    private var isFirstTime = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_search)
        binding.lifecycleOwner = this
        binding.handler = this
        if (intent.hasExtra("hash_map")) {
            map = intent.getSerializableExtra("hash_map") as HashMap<String, LeaderboardResponse>
        }
        initViewPager()
        initRecentSearchRecyclerview()
        addObserver()

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
        binding.searchView.isFocusableInTouchMode = true
        binding.searchView.requestFocus()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun addObserver() {
        searchViewModel.recentSearchLiveData.observe(
            this,
            {
                if (it.isNullOrEmpty().not()) {
                    if (binding.searchView.text.toString().isEmpty())
                        binding.recentRv.visibility = View.VISIBLE
                    else
                        binding.recentRv.visibility = View.GONE
                    itemList.clear()
                    itemList.addAll(it)
                    itemList.add(0, RecentSearch(EMPTY, Long.MAX_VALUE))
                    adapter.notifyDataSetChanged()
                } else {
                    itemList.clear()
                    binding.recentRv.visibility = View.GONE
                }
            }
        )
    }

    private fun initRecentSearchRecyclerview() {
        val linearLayoutManager =
            com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recentRv.layoutManager = linearLayoutManager
        searchViewModel.fetchRecentSearch()
        adapter = RecentSearchListAdapter(itemList, this::performSearch)
        binding.recentRv.adapter = adapter
    }

    private fun performSearch(keyword: String) {
        if (keyword.equals("clear")) {
            searchViewModel.clearResultHistory()
        } else {
            binding.searchView.setText(keyword)
            binding.recentRv.visibility = View.GONE
        }
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.adapter =
            LeaderboardSearchPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 4
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position != 3) {
                    isFirstTime = false
                }
            }
        })

        hideViewpager()

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
                4 -> {
                    list = "BATCH"
                }
                3 -> {
                    list = "LIFETIME"
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

    fun hideViewpager() {
        binding.viewPager.visibility = View.GONE
        binding.tabLayout.visibility = View.GONE
        binding.rank.visibility = View.GONE
        binding.points.visibility = View.GONE
        binding.name.visibility = View.GONE
        binding.searchLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent))
        binding.searchLayout.elevation = 8f
        binding.backIv.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.clearIv.setColorFilter(ContextCompat.getColor(this, R.color.black))
        binding.searchBg.background = ContextCompat.getDrawable(this, R.drawable.grey_rounded_bg)
        binding.recentRv.visibility = View.VISIBLE
        binding.divider.visibility = View.GONE
    }

    fun showViewpager() {
        binding.viewPager.visibility = View.VISIBLE
        binding.tabLayout.visibility = View.VISIBLE
        binding.rank.visibility = View.VISIBLE
        binding.points.visibility = View.VISIBLE
        binding.name.visibility = View.VISIBLE
        binding.searchLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        binding.searchLayout.elevation = 8f
        binding.backIv.setColorFilter(ContextCompat.getColor(this, R.color.white))
        binding.clearIv.setColorFilter(ContextCompat.getColor(this, R.color.white))
        binding.searchBg.background =
            ContextCompat.getDrawable(this, R.drawable.primary_dark_rounded_bg)
        binding.recentRv.visibility = View.GONE
        binding.divider.visibility = View.VISIBLE
        if (isFirstTime) {
            AppObjectController.uiHandler.post {
                binding.viewPager.currentItem = 3
                binding.viewPager.adapter?.notifyDataSetChanged()
            }
        }
    }

    fun clearSearchText() {
        binding.searchView.setText(EMPTY)
    }

    companion object {
        fun getSearchActivityIntent(
            context: Context,
            value: HashMap<String, LeaderboardResponse>?,
            conversationId: String? = null,
        ): Intent {
            return Intent(context, LeaderBoardSearchActivity::class.java).apply {
                putExtra("hash_map", value)
                putExtra(CONVERSATION_ID, conversationId)
            }
        }
    }
}
