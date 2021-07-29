package com.joshtalks.joshskills.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenPreviousLeaderboard
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.error.BaseConnectionErrorActivity
import com.joshtalks.joshskills.ui.leaderboard.search.LeaderBoardSearchActivity
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LeaderBoardViewPagerActivity : BaseConnectionErrorActivity() {
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }
    var mapOfVisitedPage = HashMap<Int, Int>()
    private var tabPosition = 0
    var isTooltipShow = false
    private var popupMenu: PopupMenu? = null

    val searchActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.getFullLeaderBoardData(Mentor.getInstance().getId(), getCourseId())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.slide_left_enter, R.anim.slide_left_exit)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_view_pager)
        binding.lifecycleOwner = this
        binding.handler = this
        initToolbar()
        initViewPager()
        addObserver()
        viewModel.getFullLeaderBoardData(Mentor.getInstance().getId(), getCourseId())
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    fun getCourseId(): String? {
        return intent.getStringExtra(COURSE_ID)
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
        binding.toolbarContainer.findViewById<MaterialToolbar>(R.id.toolbar).inflateMenu(R.menu.leaderboard_menu)
        binding.toolbarContainer.findViewById<MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_profile -> {
                    UserProfileActivity.startUserProfileActivity(
                        this,
                        Mentor.getInstance().getId(),
                        arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                        null,
                        USER_PROFILE_FLOW_FROM.LEADERBOARD.value,
                        conversationId =getConversationId()
                    )
                }
            }
            return@setOnMenuItemClickListener true
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

                binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {
                    }

                    override fun onPageSelected(position: Int) {
                        tabPosition = position
                        mapOfVisitedPage.put(position, mapOfVisitedPage.get(position)?.plus(1) ?: 1)
                        viewModel.engageLeaderBoardimpression(mapOfVisitedPage, position)
                    }

                    override fun onPageScrollStateChanged(state: Int) {}

                })
            }
        )

        viewModel.apiCallStatusLiveData.observe(
            this,
            Observer {
                it?.let {
                    when (it) {
                        ApiCallStatus.FAILED ->{
                            hideProgressBar()
                            isApiFalied(false,R.string.no_leaderboard_txt)
                        }
                        ApiCallStatus.SUCCESS -> {
                            isApiFalied(true)
                            hideProgressBar()
                        }
                        ApiCallStatus.START -> {
                            showProgressBar()
                        }
                    }
                }
            }
        )
    }

    fun addSearchTooltip() {
        val flag = PrefManager.getBoolValue(SEARCH_HINT_SHOW)
        if (flag) {
            return
        }
        hideProgressBar()
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
                .setOverlayColorResource(R.color.pd_transparent_bg_v2) // background color of the overlay using a color resource.
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
        val tabCount = binding.tabLayout.tabCount
        for (index in 0 until tabCount) {
            val tab = binding.tabLayout.getTabAt(index)
            when (index) {
                0 -> {
                    list = "TODAY"
                    setText(tab, map[list])
                }
                1 -> {
                    list = "WEEK"
                    setText(tab, map[list])
                }
                2 -> {
                    list = "MONTH"
                    setText(tab, map[list])
                }
                4 -> {
                    list = "BATCH"
                    if (map[list]?.intervalTabText.isNullOrBlank())
                        tab?.text = getString(R.string.my_batch)
                    else
                        tab?.text = getString(R.string.my_batch).plus('\n')
                            .plus(map[list]?.intervalTabText)
                }
                3 -> {
                    list = "LIFETIME"
                    setText(tab, map[list])
                }
            }
        }
    }

    private fun setText(tab: TabLayout.Tab?, response: LeaderboardResponse?) {
        if (response?.intervalTabText.isNullOrBlank()) {
            tab?.text =
                response?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
        } else {
            tab?.text =
                response?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
                    .plus('\n')
                    .plus(response?.intervalTabText)


        }

    }

    private fun initViewPager() {
        binding.viewPager.adapter =
            LeaderBoardViewPagerAdapter(
                getCourseId(), supportFragmentManager,
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
            )
        binding.viewPager.offscreenPageLimit = 4
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun isInternetAvailable(isInternetAvailable: Boolean) { }

    override fun onRetry() {
        supportFragmentManager.popBackStack()
        viewModel.getFullLeaderBoardData(Mentor.getInstance().getId(), getCourseId())
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
