package com.joshtalks.joshskills.ui.leaderboard

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.MainThread
import androidx.appcompat.widget.ActionBarOverlayLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
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
import com.joshtalks.joshskills.ui.leaderboard.search.LeaderBoardSearchActivity
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.joshtalks.joshskills.ui.online_test.OnlineTestFragment
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.*
import kotlinx.android.synthetic.main.acitivity_unlock_next_class_layout.card
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class LeaderBoardViewPagerActivity : WebRtcMiddlewareActivity() {
    private val TAG = "LeaderBoardViewPagerAct"
    lateinit var binding: ActivityLeaderboardViewPagerBinding
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }
    var mapOfVisitedPage = HashMap<Int, Int>()
    private var compositeDisposable = CompositeDisposable()
    private var tabPosition = 0
    var isTooltipShow = false
    private var toolTipJob : Job? = null

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
                        binding.tabOverlay.visibility = View.INVISIBLE
                        try {
                            toolTipJob?.cancel()
                        } catch (e : Exception) {
                            // Ignore the exception
                            e.printStackTrace()
                        }
                        tabPosition = position
                        hideTabOverlay()
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
                             hideTabOverlay()
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

    fun setTabOverlay(position : Int) {
        Log.d(TAG, "setTabOverlay: $position")
        toolTipJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            binding.tabOverlay.setOnClickListener(null)
            if(isActive) {
                withContext(Dispatchers.Main) {
                    val tooltipView =
                        binding.tabOverlay.findViewById<JoshTooltip>(R.id.tooltip)
                    val topLayout =
                        binding.tabOverlay.findViewById<FrameLayout>(R.id.tab_overlay_top)
                    val cardLayout =
                        binding.tabOverlay.findViewById<ConstraintLayout>(R.id.container)
                    val tabToDismissView =
                        binding.tabOverlay.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
                    //val overlayContainer = binding.tabOverlay.findViewById<RelativeLayout>(R.id.tab_bar_overlay_container)
                    when(position) {
                        in 0..2 -> {
                            showWinnerOverlay(position, topLayout, cardLayout)
                            showToolTip(tooltipView, tooltipTextList[position])
                            showTapToDismiss(topLayout, cardLayout, tabToDismissView)
                        }
                        3 -> {
                            binding.tabOverlay.visibility = View.VISIBLE
                            cardLayout.visibility = GONE
                            tooltipView.visibility = GONE
                            //showToolTip(tooltipView, tooltipTextList[position])
                            showTapToDismiss(topLayout, cardLayout, tabToDismissView)
                        }

                        4 -> {
                            binding.tabOverlay.visibility = View.VISIBLE
                            cardLayout.visibility = GONE
                            tooltipView.y = getFinalYAxis().toFloat()
                            tooltipView.requestLayout()
                            showToolTip(tooltipView, tooltipTextList[position])
                            showTapToDismiss(topLayout, cardLayout, tabToDismissView)
                        }
                    }
                }
            }
        }
    }

    private fun showWinnerOverlay(position : Int, topLayout : FrameLayout, cardLayout : ConstraintLayout) {
        getOverlayData(position)?.let {
            val OFFSET =
                (getScreenHeightAndWidth().first - binding.parentContainer.height).toFloat()
            val VIEW_PADDING_PX = (8 * getConverterValue()).toInt()
            val FIRST_ELEMENT_OFFSET = resources.getDimension(R.dimen._4sdp)
            val tabPosition = IntArray(2)
            val tab = binding.tabLayout.getTabAt(position)
            val width = tab?.view!!.width
            val height = tab.view.height
            val tabText = tab.view.tab?.text
            Log.d(TAG, "setTabOverlay: $tabText")
            //x -- left
            // y -- top
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
            topLayout.layoutParams.height = height + VIEW_PADDING_PX
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
            topLayout.y = tabPosition[1].toFloat() - OFFSET
            topLayout.requestLayout()
            val topLayoutRect = Rect()
            topLayout.getGlobalVisibleRect(topLayoutRect)
            cardLayout.y = (topLayoutRect.bottom.toFloat() - OFFSET)
            cardLayout.requestLayout()
            cardLayout.visibility = View.VISIBLE
            topLayout.visibility = View.VISIBLE
            binding.tabOverlay.visibility = View.VISIBLE
        }
    }

    fun getFinalYAxis() : Double {
        val height = getScreenHeightAndWidth().first
        val OFFSET =
            ( height - binding.parentContainer.height).toFloat()
       return ((height * 0.20) - OFFSET)
    }

    @MainThread
    private suspend fun showToolTip(tooltipView : JoshTooltip, tooltipText : String) {
        delay(300)
        tooltipView.setTooltipText(tooltipText)
        tooltipView.visibility = View.VISIBLE
        tooltipView.startAnimation(
            AnimationUtils.loadAnimation(
                this,
                R.anim.slide_in_right
            )
        )
    }

    @MainThread
    private suspend fun showTapToDismiss(topLayout : FrameLayout, cardLayout : ConstraintLayout, labelTapToDismiss : AppCompatTextView) {
        fun setDismissListener() {
            fun removeListener() {
                labelTapToDismiss.setOnClickListener(null)
                topLayout.setOnClickListener(null)
                cardLayout.setOnClickListener(null)
                binding.tabOverlay.setOnClickListener(null)
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

    
    private fun getOverlayData(position: Int) = when(position) {
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
        tooltipView.visibility = View.INVISIBLE
        topLayout.visibility = View.INVISIBLE
        cardLayout.visibility = View.INVISIBLE
        tabToDismissView.visibility = View.INVISIBLE
        binding.tabOverlay.visibility = View.INVISIBLE
    }

    @MainThread
    fun setOverlayData(root: ConstraintLayout, response : LeaderboardMentor) {
        var title : TextView
        var name : TextView
        var award : ImageView
        var userPic : CircleImageView
        var onlineStatusLayout : FrameLayout
        var points : TextView
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
            userPic.setUserImageOrInitials(response.photoUrl, response.name?:"User")
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
}
