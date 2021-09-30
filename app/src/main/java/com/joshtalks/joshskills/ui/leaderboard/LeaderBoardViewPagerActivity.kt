package com.joshtalks.joshskills.ui.leaderboard

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.core.videotranscoder.recyclerView
import com.joshtalks.joshskills.databinding.ActivityLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenPreviousLeaderboard
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_LEADERBOARD_BATCH_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_LEADERBOARD_ITEM_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_LEADERBOARD_LIFETIME_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_MONTHS_WINNER_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_TODAYS_WINNER_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_WEEKS_WINNER_ANIMATION
import com.joshtalks.joshskills.ui.leaderboard.constants.NEED_VIEW_BITMAP
import com.joshtalks.joshskills.ui.leaderboard.constants.PROFILE_ITEM_CLICKED
import com.joshtalks.joshskills.ui.leaderboard.constants.SCROLL_TO_TOP
import com.joshtalks.joshskills.ui.leaderboard.search.LeaderBoardSearchActivity
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LeaderBoardViewPagerActivity : WebRtcMiddlewareActivity(), ViewBitmap {
    private val TAG = "LeaderBoardViewPagerAct"
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }
    var mapOfVisitedPage = HashMap<Int, Int>()
    private var compositeDisposable = CompositeDisposable()
    private var tabPosition = 0
    var isTooltipShow = false
    private var toolTipJob: Job? = null
    private var currentAimation: Int? = null
    private var ITEM_ANIMATION = 1

    val searchActivityResult: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.getFullLeaderBoardData(Mentor.getInstance().getId(), getCourseId())
            }
        }

    companion object {
        val winnerMap = mutableMapOf<String, LeaderboardMentor>()
        val tooltipTextList = mutableListOf(
            "जो student एक दिन में सबसे ज़्यादा मेहनत करता है वह Student of the Day बनता है",
            "जो student एक हफ्ते में सबसे ज़्यादा मेहनत करता है वह Student of the Week बनता है",
            "जो student एक महीने में सबसे ज़्यादा मेहनत करता है वह Student of the Month बनता है",
            "",
            "बैच: यानी कि आपका समूह। वह सारे students जिन्होंने आपके साथ 10th April को यह कोर्स शुरू किया.",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_leaderboard_view_pager)
        binding.lifecycleOwner = this
        binding.handler = this
        initToolbar()
        initViewPager()
        addObserver()
        /*PrefManager.put(HAS_SEEN_TODAYS_WINNER_ANIMATION, false)
        PrefManager.put(HAS_SEEN_WEEKS_WINNER_ANIMATION, false)
        PrefManager.put(HAS_SEEN_MONTHS_WINNER_ANIMATION, false)
        PrefManager.put(HAS_SEEN_LEADERBOARD_BATCH_ANIMATION, false)
        PrefManager.put(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION, false)*/
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
        val currentMentor = viewModel.leaderBoardData.value?.get("TODAY")?.current_mentor
        val isCourseBought = currentMentor?.isCourseBought ?: false
        searchActivityResult.launch(
            LeaderBoardSearchActivity.getSearchActivityIntent(
                this,
                viewModel.leaderBoardData.value,
                intent.getStringExtra(CONVERSATION_ID),
                isCourseBought,
                currentMentor?.expiryDate?.time
            )
        )
    }

    private fun addObserver() {
        viewModel.leaderBoardData.observe(
            this, {
                mapOfVisitedPage[0] = 0
                mapOfVisitedPage[1] = 0
                mapOfVisitedPage[2] = 0

                setTabText(it)

                val currentMentor = it["TODAY"]?.current_mentor
                if (currentMentor?.isCourseBought == false &&
                    currentMentor.expiryDate != null &&
                    currentMentor.expiryDate.time < System.currentTimeMillis()
                ) {
                    binding.freeTrialExpiryLayout.visibility = VISIBLE
                } else {
                    binding.freeTrialExpiryLayout.visibility = GONE
                }

                binding.viewPager.registerOnPageChangeCallback(object :
                    ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding.tabOverlay.visibility = View.INVISIBLE
                        try {
                            toolTipJob?.cancel()
                        } catch (e: Exception) {
                            // Ignore the exception
                            e.printStackTrace()
                        }
                        tabPosition = position
                        hideTabOverlay()
                        hideItemTabOverlay()
                        if (!(PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION) &&
                                    PrefManager.getBoolValue(HAS_SEEN_TODAYS_WINNER_ANIMATION) &&
                                    PrefManager.getBoolValue(HAS_SEEN_WEEKS_WINNER_ANIMATION) &&
                                    PrefManager.getBoolValue(HAS_SEEN_MONTHS_WINNER_ANIMATION) &&
                                    PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_BATCH_ANIMATION) &&
                                    PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_LIFETIME_ANIMATION))
                        )
                            setTabOverlay(tabPosition)
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
                        ApiCallStatus.SUCCESS -> {
                            hideProgressBar()
                            hideItemTabOverlay()
                            hideTabOverlay()
                            if (!(PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION) &&
                                        PrefManager.getBoolValue(HAS_SEEN_TODAYS_WINNER_ANIMATION))
                            )
                                setTabOverlay(0)
                        }
                        ApiCallStatus.FAILED -> {
                            hideProgressBar()
                        }
                        ApiCallStatus.START -> {
                            showProgressBar()
                        }
                    }
                }
            }
        )

        /*viewModel.overlayLiveData.observe(this) {
            it?.let {
                setTabOverlay(it)
            }
        }*/
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
                if (position == 4) {
                    tab.text = getString(R.string.my_batch)
                } else {
                    tab.text =
                        map.get(list)?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
                }
            } else {
                if (position == 4) {
                    tab.text = getString(R.string.my_batch).plus('\n')
                        .plus(map.get(list)?.intervalTabText)
                } else {
                    tab.text =
                        map.get(list)?.intervalType?.toLowerCase(Locale.getDefault())?.capitalize()
                            .plus('\n')
                            .plus(map.get(list)?.intervalTabText)
                }

            }
        }.attach()
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPager.isUserInputEnabled = true
        binding.viewPager.adapter =
            LeaderBoardViewPagerAdapter(getCourseId(), this)
        binding.viewPager.offscreenPageLimit = 4
        binding.viewPager.recyclerView.enforceSingleScrollDirection()
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

    fun showFreeTrialPaymentScreen() {
        FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            viewModel.leaderBoardData.value?.get("TODAY")?.current_mentor?.expiryDate?.time
        )
        // finish()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    //fun calculateTabBarDetails(index : Int) = binding.tabLayout.getTabWidth(index)

    private fun getConverterValue(): Float {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        val logicalDensity: Float = metrics.density
        return logicalDensity
    }

    fun getScreenHeightAndWidth(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels to metrics.widthPixels
    }

    fun setTabOverlay(position: Int) {
        Log.d(TAG, "setTabOverlay: $position")
        toolTipJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            withContext(Dispatchers.Main) {
                binding.tabOverlay.setOnClickListener(null)
            }
            if (isActive) {
                withContext(Dispatchers.Main) {
                    if (PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION)) {
                        viewModel.eventLiveData.postValue(
                            Event(
                                eventType = SCROLL_TO_TOP,
                                type = getTabName(position)
                            )
                        )
                        delay(100)
                        val tooltipView =
                            binding.tabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
                        val batchTooltipView =
                            binding.tabOverlay.findViewById<JoshTooltip>(R.id.batch_tooltip)
                        val topLayout =
                            binding.tabOverlay.findViewById<FrameLayout>(R.id.tab_overlay_top)
                        val cardLayout =
                            binding.tabOverlay.findViewById<ConstraintLayout>(R.id.container)
                        val tabToDismissView =
                            binding.tabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
                        val swipeAnimationView =
                            binding.tabOverlay.findViewById<LottieAnimationView>(R.id.swipe_hint)
                        swipeAnimationView.visibility = INVISIBLE
                        swipeAnimationView.cancelAnimation()
                        when (position) {
                            0 -> {
                                if (!PrefManager.getBoolValue(HAS_SEEN_TODAYS_WINNER_ANIMATION)) {
                                    batchTooltipView.visibility = INVISIBLE
                                    showWinnerOverlay(position, topLayout, cardLayout, tooltipView)
                                    currentAimation = ITEM_ANIMATION
                                    PrefManager.put(HAS_SEEN_TODAYS_WINNER_ANIMATION, true)
                                    binding.tabOverlay.setOnClickListener(null)
                                    delay(2000)
                                    swipeAnimationView.visibility = VISIBLE
                                    swipeAnimationView.playAnimation()
                                    currentAimation = ITEM_ANIMATION
                                    binding.tabOverlay.isClickable = false
                                    /*showTapToDismiss(
                                        topLayout,
                                        cardLayout,
                                        tabToDismissView,
                                        position
                                    )*/
                                }
                            }
                            1 -> {
                                if (!PrefManager.getBoolValue(HAS_SEEN_WEEKS_WINNER_ANIMATION)) {
                                    batchTooltipView.visibility = INVISIBLE
                                    showWinnerOverlay(position, topLayout, cardLayout, tooltipView)
                                    currentAimation = ITEM_ANIMATION
                                    PrefManager.put(HAS_SEEN_WEEKS_WINNER_ANIMATION, true)
                                    /*showTapToDismiss(
                                        topLayout,
                                        cardLayout,
                                        tabToDismissView,
                                        position
                                    )*/
                                    binding.tabOverlay.setOnClickListener(null)
                                    delay(2000)
                                    swipeAnimationView.visibility = VISIBLE
                                    swipeAnimationView.playAnimation()
                                    currentAimation = ITEM_ANIMATION
                                    binding.tabOverlay.isClickable = false
                                }
                            }
                            2 -> {
                                if (!PrefManager.getBoolValue(HAS_SEEN_MONTHS_WINNER_ANIMATION)) {
                                    batchTooltipView.visibility = INVISIBLE
                                    showWinnerOverlay(position, topLayout, cardLayout, tooltipView)
                                    currentAimation = ITEM_ANIMATION
                                    PrefManager.put(HAS_SEEN_MONTHS_WINNER_ANIMATION, true)
                                    /*showTapToDismiss(
                                        topLayout,
                                        cardLayout,
                                        tabToDismissView,
                                        position
                                    )*/
                                    binding.tabOverlay.setOnClickListener(null)
                                    delay(2000)
                                    swipeAnimationView.visibility = VISIBLE
                                    swipeAnimationView.playAnimation()
                                    currentAimation = ITEM_ANIMATION
                                    binding.tabOverlay.isClickable = false
                                }
                            }
                            3 -> {
                                if (!PrefManager.getBoolValue(
                                        HAS_SEEN_LEADERBOARD_LIFETIME_ANIMATION
                                    )
                                ) {
                                    binding.tabOverlay.visibility = View.VISIBLE
                                    cardLayout.visibility = GONE
                                    tooltipView.visibility = GONE
                                    batchTooltipView.visibility = GONE
                                    swipeAnimationView.visibility = VISIBLE
                                    swipeAnimationView.playAnimation()
                                    PrefManager.put(HAS_SEEN_LEADERBOARD_LIFETIME_ANIMATION, true)
                                    currentAimation = ITEM_ANIMATION
                                    binding.tabOverlay.isClickable = false
                                    /*showTapToDismiss(
                                        topLayout,
                                        cardLayout,
                                        tabToDismissView,
                                        position
                                    )*/
                                }
                            }

                            4 -> {
                                if (!PrefManager.getBoolValue(
                                        HAS_SEEN_LEADERBOARD_BATCH_ANIMATION
                                    )
                                ) {
                                    binding.tabOverlay.visibility = View.VISIBLE
                                    cardLayout.visibility = GONE
                                    swipeAnimationView.visibility = GONE
                                    showToolTip(batchTooltipView, tooltipTextList[position])
                                    PrefManager.put(HAS_SEEN_LEADERBOARD_BATCH_ANIMATION, true)
                                    currentAimation = ITEM_ANIMATION
                                    showTapToDismiss(
                                        topLayout,
                                        cardLayout,
                                        tabToDismissView,
                                        position
                                    )
                                }
                            }
                        }
                    } else {
                        //if(position == 0) {
                        delay(1250)
                        setRecyclerViewItemAnimation(position)
                        //getView(position)
                        //}
                    }
                }
            }
        }
    }

    private fun showWinnerOverlay(
        position: Int,
        topLayout: FrameLayout,
        cardLayout: ConstraintLayout,
        tooltipView: JoshTooltip
    ) {
        getOverlayData(position)?.let {
            val STATUS_BAR_HEIGHT = getStatusBarHeight()
            val VIEW_PADDING_PX = resources.getDimension(R.dimen._8sdp)
            val FIRST_ELEMENT_OFFSET = resources.getDimension(R.dimen._4sdp)
            val tabPosition = IntArray(2)
            val tab = binding.tabLayout.getTabAt(position)
            val width = tab?.view!!.width
            val height = tab.view.height
            val tabText = tab.view.tab?.text
            Log.d(TAG, "setTabOverlay: $tabText")
            //tab.view.getLocationInWindow(tabPosition)
            tab.view.getLocationOnScreen(tabPosition)
            val tabView =
                binding.tabOverlay.findViewById<AppCompatTextView>(R.id.tab_bar_text)
            tabView.text = tabText
            if (position in 1..2)
                tabView.textSize = 13f
            else
                tabView.textSize = 18f

            setOverlayData(cardLayout, it)
            topLayout.layoutParams.width = width
            topLayout.layoutParams.height = (height + VIEW_PADDING_PX).toInt()
            if (position == 0) {
                topLayout.x = tabPosition[0].toFloat() + FIRST_ELEMENT_OFFSET
                cardLayout.background = ContextCompat.getDrawable(
                    this@LeaderBoardViewPagerActivity,
                    R.drawable.winner_tooltip_first_element_background
                )
            } else {
                topLayout.x = tabPosition[0].toFloat()
                cardLayout.background = ContextCompat.getDrawable(
                    this@LeaderBoardViewPagerActivity,
                    R.drawable.winner_tooltip_background
                )
            }
            topLayout.y = tabPosition[1].toFloat() - STATUS_BAR_HEIGHT
            topLayout.requestLayout()
            val topLayoutRect = Rect()
            topLayout.getGlobalVisibleRect(topLayoutRect)
            cardLayout.y = topLayout.y + topLayout.height
            cardLayout.requestLayout()
            val alphaView = binding.tabOverlay.findViewById<FrameLayout>(R.id.tab_overlay_top_alpha)
            val alphaOverlayView =
                binding.tabOverlay.findViewById<FrameLayout>(R.id.winner_card_overlay_container)
            val overlayPosition = IntArray(2)
            alphaOverlayView.visibility = INVISIBLE
            alphaOverlayView.layoutParams.width = cardLayout.width
            alphaOverlayView.layoutParams.height = cardLayout.height
            cardLayout.getLocationOnScreen(overlayPosition)
            alphaOverlayView.x = overlayPosition[0].toFloat()
            alphaOverlayView.y = overlayPosition[1].toFloat() - STATUS_BAR_HEIGHT
            alphaOverlayView.requestLayout()
            alphaOverlayView.visibility = View.VISIBLE
            alphaView.visibility = View.VISIBLE
            cardLayout.visibility = View.VISIBLE
            topLayout.visibility = View.VISIBLE
            cardLayout.setOnClickListener {
                binding.tabOverlay.visibility = View.INVISIBLE
                when (position) {
                    0 -> RxBus2.publish(OpenPreviousLeaderboard("TODAY"))
                    1 -> RxBus2.publish(OpenPreviousLeaderboard("WEEK"))
                    2 -> RxBus2.publish(OpenPreviousLeaderboard("MONTH"))
                }
            }
            binding.tabOverlay.visibility = View.VISIBLE
            animateAlpha(
                alphaView,
                alphaOverlayView,
                1f,
                0f,
                tooltipView,
                tooltipTextList[position]
            )
        }
    }

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).getTop()
        val titleBarHeight = contentViewTop - statusBarHeight
        Log.d(TAG, "getStatusBarHeight: $titleBarHeight")
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }
    /*fun getView(position : Int) {
        val viewPagerChild = binding.viewPager.getChildAt(position)
        Log.d(TAG, "getView: $viewPagerChild")
        val view = viewPagerChild.findViewById<PlaceHolderView>(R.id.recycler_view)
        val view1 = view.getChildAt(3)
        Log.d(TAG, "needBitmapLiveData : $view1")
        onViewBitmap(TooltipUtils.getOverlayItemFromView(view), )
        //listener?.onViewBitmap(TooltipUtils.getOverlayItemFromView(view))
    }*/

    fun animateAlpha(
        alphaView: View,
        alphaOverlayView: View,
        start: Float,
        end: Float,
        tooltipView: JoshTooltip,
        tooltipText: String
    ) {

        ValueAnimator.ofFloat(start, end).apply {
            duration = 700
            addUpdateListener {
                alphaView.alpha = it.animatedValue as Float
                alphaOverlayView.alpha = it.animatedValue as Float
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    showToolTip(tooltipView, tooltipText)
                }

                override fun onAnimationEnd(animation: Animator?) {}

                override fun onAnimationCancel(animation: Animator?) {}

                override fun onAnimationRepeat(animation: Animator?) {}
            })
        }.start()
    }

    @MainThread
    private fun showToolTip(tooltipView: JoshTooltip, tooltipText: String) {
        tooltipView.setTooltipText(tooltipText)
        slideInAnimation(tooltipView)
    }

    @MainThread
    private suspend fun showTapToDismiss(
        topLayout: FrameLayout,
        cardLayout: ConstraintLayout,
        labelTapToDismiss: AppCompatTextView,
        position: Int
    ) {
        fun setDismissListener() {
            fun removeListener() {
                labelTapToDismiss.setOnClickListener(null)
                topLayout.setOnClickListener(null)
                cardLayout.setOnClickListener(null)
                binding.tabOverlay.setOnClickListener(null)
                currentAimation = ITEM_ANIMATION
            }

            labelTapToDismiss.setOnClickListener {
                binding.tabOverlay.visibility = View.INVISIBLE
                removeListener()
            }
            topLayout.setOnClickListener {
                binding.tabOverlay.visibility = View.INVISIBLE
                removeListener()
            }

            cardLayout.setOnClickListener {
                binding.tabOverlay.visibility = View.INVISIBLE
                when (position) {
                    0 -> RxBus2.publish(OpenPreviousLeaderboard("TODAY"))
                    1 -> RxBus2.publish(OpenPreviousLeaderboard("WEEK"))
                    2 -> RxBus2.publish(OpenPreviousLeaderboard("MONTH"))
                }
                removeListener()
            }

            binding.tabOverlay.setOnClickListener {
                binding.tabOverlay.visibility = View.INVISIBLE
                removeListener()
            }
        }
        delay(6500)
        setDismissListener()
        labelTapToDismiss.visibility = View.VISIBLE
        labelTapToDismiss.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.slide_up_dialog)
        )
    }

    @MainThread
    private suspend fun showTapToDismiss(
        labelTapToDismiss: AppCompatTextView,
        arrowView: LottieAnimationView,
        tooltipView: JoshTooltip
    ) {
        withContext(Dispatchers.Main) {
            labelTapToDismiss.visibility = View.INVISIBLE
            fun setDismissListener() {
                fun removeListener() {
                    labelTapToDismiss.setOnClickListener(null)
                    arrowView.setOnClickListener(null)
                    tooltipView.setOnClickListener(null)
                    binding.itemTabOverlay.setOnClickListener(null)
                }

                labelTapToDismiss.setOnClickListener {
                    binding.tabOverlay.visibility = View.INVISIBLE
                    removeListener()
                }
                arrowView.setOnClickListener {
                    binding.itemTabOverlay.visibility = View.INVISIBLE
                    removeListener()
                }

                tooltipView.setOnClickListener {
                    binding.itemTabOverlay.visibility = View.INVISIBLE
                    removeListener()
                }

                binding.itemTabOverlay.setOnClickListener {
                    binding.itemTabOverlay.visibility = View.INVISIBLE
                    removeListener()
                }
            }
            delay(6500)
            setDismissListener()
            labelTapToDismiss.visibility = View.VISIBLE
            labelTapToDismiss.startAnimation(
                AnimationUtils.loadAnimation(
                    this@LeaderBoardViewPagerActivity,
                    R.anim.slide_up_dialog
                )
            )
        }
    }

    private fun getOverlayData(position: Int) = when (position) {
        0 -> winnerMap["TODAY"]
        1 -> winnerMap["WEEK"]
        2 -> winnerMap["MONTH"]
        else -> null
    }

    private fun hideTabOverlay() {
        val tooltipView =
            binding.tabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
        val topLayout =
            binding.tabOverlay.findViewById<FrameLayout>(R.id.tab_overlay_top)
        val cardLayout =
            binding.tabOverlay.findViewById<ConstraintLayout>(R.id.container)
        val tabToDismissView =
            binding.tabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
        val swipeAnimationView =
            binding.tabOverlay.findViewById<LottieAnimationView>(R.id.swipe_hint)
        val alphaView = binding.tabOverlay.findViewById<FrameLayout>(R.id.tab_overlay_top_alpha)
        val alphaOverlayView =
            binding.tabOverlay.findViewById<FrameLayout>(R.id.winner_card_overlay_container)
        val batchTooltipView =
            binding.tabOverlay.findViewById<JoshTooltip>(R.id.batch_tooltip)
        tooltipView.visibility = View.INVISIBLE
        topLayout.visibility = View.INVISIBLE
        cardLayout.visibility = View.INVISIBLE
        tabToDismissView.visibility = View.INVISIBLE
        alphaView.visibility = INVISIBLE
        swipeAnimationView.visibility = GONE
        batchTooltipView.visibility = INVISIBLE
        alphaOverlayView.visibility = INVISIBLE
        binding.tabOverlay.visibility = View.INVISIBLE
    }

    private fun hideItemTabOverlay() {
        val tooltipView =
            binding.itemTabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
        val itemImageView =
            binding.itemTabOverlay.findViewById<ImageView>(R.id.profile_item_image)
        val arrowView = binding.itemTabOverlay.findViewById<ImageView>(R.id.arrow_animation)
        val tabToDismissView =
            binding.itemTabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
        tooltipView.visibility = View.INVISIBLE
        tabToDismissView.visibility = View.INVISIBLE
        itemImageView.visibility = View.INVISIBLE
        arrowView.visibility = View.INVISIBLE
        binding.itemTabOverlay.visibility = View.INVISIBLE
        currentAimation = null
    }

    @MainThread
    fun setOverlayData(root: ConstraintLayout, response: LeaderboardMentor) {
        var title: TextView
        var name: TextView
        var award: ImageView
        var userPic: CircleImageView
        var onlineStatusLayout: FrameLayout
        var points: TextView
        with(root) {
            title = findViewById(R.id.title)
            name = findViewById(R.id.name)
            award = findViewById(R.id.award)
            userPic = findViewById(R.id.user_pic)
            onlineStatusLayout = findViewById(R.id.online_status_iv)
            points = findViewById(R.id.points)
        }

        title.text = response.title.toString()
        val resp = StringBuilder()
        response.name?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        name.text = resp
        points.text = (response.points.toString()).plus(" points")
        userPic.post {
            userPic.setUserImageOrInitials(response.photoUrl, response.name ?: "User")
        }
        response.award_url?.let {
            award.setImage(it)
        }
        if (response.isOnline) {
            onlineStatusLayout.visibility = View.VISIBLE
        } else {
            onlineStatusLayout.visibility = View.GONE
        }
    }

    fun setRecyclerViewItemAnimation(position: Int) {
        if (!PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION))
            showOverlayLayoutForItem(position)
    }

    fun showOverlayLayoutForItem(position: Int) {
        val itemImageView =
            binding.itemTabOverlay.findViewById<ImageView>(R.id.profile_item_image)
        val arrowView =
            binding.itemTabOverlay.findViewById<LottieAnimationView>(R.id.arrow_animation)
        val tooltipView = binding.itemTabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
        val tapToDismissView =
            binding.itemTabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
        binding.itemTabOverlay.visibility = VISIBLE
        binding.itemTabOverlay.setOnClickListener(null)
        arrowView.visibility = INVISIBLE
        itemImageView.visibility = INVISIBLE
        tooltipView.visibility = INVISIBLE
        tapToDismissView.visibility = INVISIBLE
        viewModel.eventLiveData.postValue(
            Event(
                eventType = NEED_VIEW_BITMAP,
                type = getTabName(position)
            )
        )
    }

    override fun onViewBitmap(overlayItem: ItemOverlay, type: String, arrowPosition: Float?) {
        Log.d(TAG, "onViewBitmap: $overlayItem")
        val OFFSET = getStatusBarHeight()
        val itemImageView =
            binding.itemTabOverlay.findViewById<ImageView>(R.id.profile_item_image)
        val arrowView =
            binding.itemTabOverlay.findViewById<LottieAnimationView>(R.id.arrow_animation)
        val tooltipView = binding.itemTabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
        val tapToDismissView =
            binding.itemTabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
        itemImageView.setImageBitmap(overlayItem.viewBitmap)
        arrowView.x = (arrowPosition
            ?: (getScreenHeightAndWidth().second / 2.0).toFloat()).toFloat() - resources.getDimension(
            R.dimen._40sdp
        )
        arrowView.y = overlayItem.y.toFloat() - OFFSET - resources.getDimension(R.dimen._32sdp)
        itemImageView.x = overlayItem.x.toFloat()
        itemImageView.y = overlayItem.y.toFloat() - OFFSET
        itemImageView.setOnClickListener {
            hideItemTabOverlay()
            viewModel.eventLiveData.postValue(Event(eventType = PROFILE_ITEM_CLICKED, type = type))
        }
        itemImageView.requestLayout()
        arrowView.requestLayout()
        binding.itemTabOverlay.visibility = VISIBLE
        arrowView.visibility = VISIBLE
        itemImageView.visibility = VISIBLE
        tooltipView.setTooltipText("आप किसी की भी Profile खोल सकते हैं")
        slideInAnimation(tooltipView)
        CoroutineScope(Dispatchers.IO).launch {
            showTapToDismiss(tapToDismissView, arrowView, tooltipView)
        }
        currentAimation = ITEM_ANIMATION
        PrefManager.put(HAS_SEEN_LEADERBOARD_ITEM_ANIMATION, true)
    }

    fun slideInAnimation(tooltipView: View) {
        tooltipView.visibility = INVISIBLE
        val start = getScreenHeightAndWidth().second
        val mid = start * 0.2 * -1
        val end = tooltipView.x
        tooltipView.x = start.toFloat()
        tooltipView.requestLayout()
        tooltipView.visibility = VISIBLE
        val valueAnimation = ValueAnimator.ofFloat(start.toFloat(), mid.toFloat(), end).apply {
            interpolator = AccelerateInterpolator()
            duration = 500
            addUpdateListener {
                tooltipView.x = it.animatedValue as Float
                tooltipView.requestLayout()
            }
        }
        valueAnimation.start()
    }

    override fun onBackPressed() {
        if (currentAimation == null) {
            super.onBackPressed()
        } else {
            hideItemTabOverlay()
            hideTabOverlay()
            currentAimation = null
        }
    }

    fun getTabName(position: Int) = when (position) {
        0 -> "TODAY"
        1 -> "WEEK"
        2 -> "MONTH"
        3 -> "LIFETIME"
        4 -> "BATCH"
        else -> ""
    }
}

data class ItemOverlay(val viewBitmap: Bitmap, val x: Int, val y: Int)

data class Event(val eventType: Int, val type: String)