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
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenPreviousLeaderboard
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.leaderboard.search.LeaderBoardSearchActivity
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class LeaderBoardViewPagerActivity : WebRtcMiddlewareActivity() {
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }
    var mapOfVisitedPage = HashMap<Int, Int>()
    private var compositeDisposable = CompositeDisposable()
    private var tabPosition = 0
    var isTooltipShow = false

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

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
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
            visibility = View.VISIBLE
            setImageDrawable(
                ContextCompat.getDrawable(
                    this@LeaderBoardViewPagerActivity,
                    R.drawable.ic_search
                )
            )
            setOnClickListener { openSearchActivity() }
        }
        text_message_title.text = getString(R.string.leaderboard)
        lifecycleScope.launch(Dispatchers.Default) {
            PrefManager.put(
                LEADER_BOARD_OPEN_COUNT,
                (PrefManager.getIntValue(LEADER_BOARD_OPEN_COUNT) + 1)
            )
        }
    }

    private fun openSearchActivity() {
        searchActivityResult.launch(
            LeaderBoardSearchActivity.getSearchActivityIntent(
                this,
                viewModel.leaderBoardData.value,
                intent.getStringExtra(CONVERSATION_ID)
            )
        )
    }

    private fun addObserver() {
        viewModel.leaderBoardData.observe(
            this,
            Observer {
                mapOfVisitedPage.put(0, 0)
                mapOfVisitedPage.put(1, 0)
                mapOfVisitedPage.put(2, 0)

                setTabText(it)

                binding.viewPager.registerOnPageChangeCallback(object :
                        ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)
                            tabPosition = position
                            mapOfVisitedPage.put(position, mapOfVisitedPage.get(position)?.plus(1) ?: 1)
                            viewModel.engageLeaderBoardimpression(mapOfVisitedPage, position)
                        }
                    })
            }
        )

        viewModel.apiCallStatusLiveData.observe(
            this,
            Observer {
                it?.let {
                    when (it) {
                        ApiCallStatus.FAILED, ApiCallStatus.SUCCESS -> {
                            hideProgressBar()
                            addSearchTooltip()
                        }
                        ApiCallStatus.START -> {
                            showProgressBar()
                        }
                    }
                }
            }
        )
    }

    private fun addSearchTooltip() {
        val flag = PrefManager.getBoolValue(SEARCH_HINT_SHOW)
        if (flag) {
            return
        }
        val lbOpenCount = PrefManager.getIntValue(LEADER_BOARD_OPEN_COUNT)
        val isLastCall = PrefManager.getBoolValue(P2P_LAST_CALL)
        if (lbOpenCount >= 4 || isLastCall) {
            val balloon = Balloon.Builder(this)
                .setText(getString(R.string.search_tooltip))
                .setTextSize(15F)
                .setTextColor(ContextCompat.getColor(this, R.color.black))
                .setArrowOrientation(ArrowOrientation.TOP)
                .setDismissWhenTouchOutside(true)
                .setCornerRadius(10f)
                .setWidthRatio(0.85f)
                .setArrowPosition(0.82f)
                .setPadding(8)
                .setMarginTop(12)
                .setIsVisibleOverlay(true) // sets the visibility of the overlay for highlighting an anchor.
                .setOverlayColorResource(R.color.pd_transparent_bg) // background color of the overlay using a color resource.
                .setOverlayPadding(4f) // sets a padding value of the overlay shape in
                .setBalloonOverlayAnimation(BalloonOverlayAnimation.FADE) // default is fade.
                .setDismissWhenOverlayClicked(false) // disable di
                .setBackgroundColorResource(R.color.white)
                .setBalloonAnimation(BalloonAnimation.CIRCULAR)
                .setLifecycleOwner(this)
                .setDismissWhenClicked(true)
                .setArrowOrientation(ArrowOrientation.BOTTOM)
                .build()
            balloon.showAlignBottom(iv_earn)
            PrefManager.put(SEARCH_HINT_SHOW, true)
            isTooltipShow = true
        }
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

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.adapter =
            LeaderBoardViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 4
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenPreviousLeaderboard::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        var type = EMPTY
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
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun openPreviousLeaderBoard(intervalType: String) {
        PreviousLeaderboardActivity.startPreviousLeaderboardActivity(
            this,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            intervalType,
            conversationId = intent.getStringExtra(CONVERSATION_ID)
        )
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
