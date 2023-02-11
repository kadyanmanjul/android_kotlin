package com.joshtalks.joshskills.premium.ui.points_history

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.play.core.splitcompat.SplitCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.databinding.ActivityPointsHistoryBinding
import com.joshtalks.joshskills.premium.track.CONVERSATION_ID
import com.joshtalks.joshskills.premium.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.premium.ui.leaderboard.constants.HAS_SEEN_POINTS_HISTORY_ANIMATION
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.premium.ui.points_history.viewholder.PointsSummaryDescViewHolder
import com.joshtalks.joshskills.premium.ui.points_history.viewholder.PointsSummaryTitleViewHolder
import com.joshtalks.joshskills.premium.ui.points_history.viewmodel.PointsViewModel
import com.joshtalks.joshskills.premium.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.premium.ui.tooltip.TooltipUtils
import kotlinx.android.synthetic.main.base_toolbar.*
import kotlinx.coroutines.*
import java.text.DecimalFormat

const val MENTOR_ID = "mentor_id"
private const val TAG = "PointsHistoryActivity"

class PointsHistoryActivity : CoreJoshActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityPointsHistoryBinding
    private var mentorId: String? = null
    private var isAnimationVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra(MENTOR_ID)) {
            mentorId = intent.getStringExtra(MENTOR_ID)
        }
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_points_history)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        initToolbar()
        viewModel.getPointsSummary(mentorId)
        showProgressBar()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initToolbar() {
        with(iv_back) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
                onBackPressed()
            }
        }
        with(iv_help) {
            visibility = View.VISIBLE
            setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                openHelpActivity()
            }
        }
        text_message_title.text = getString(R.string.points_history)
    }

    private fun addObserver() {
        viewModel.pointsHistoryLiveData.observe(
            this, {
                binding.userScore.text = DecimalFormat("#,##,##,###").format(it.totalPoints)
                binding.userScoreText.text = it.totalPointsText

                if (it.isCourseBought.not() &&
                    it.expiryDate != null &&
                    it.expiryDate.time < System.currentTimeMillis()
                ) {
                    binding.freeTrialExpiryLayout.visibility = View.VISIBLE
                } else {
                    binding.freeTrialExpiryLayout.visibility = View.GONE
                }

                it.pointsHistoryDateList?.forEachIndexed { index, list ->
                    if (list.pointsSum != null) {
                        binding.recyclerView.addView(
                            PointsSummaryTitleViewHolder(
                                list.date!!,
                                list.pointsSum,
                                list.awardIconList,
                                index
                            )
                        )
                        list.pointsHistoryList?.forEachIndexed { index, pointsHistory ->
                            binding.recyclerView.addView(
                                PointsSummaryDescViewHolder(
                                    pointsHistory,
                                    index,
                                    list.pointsHistoryList.size
                                )
                            )
                        }
                    }
                }
            }
        )

        viewModel.apiCallStatusLiveData.observe(
            this,
            Observer {
                hideProgressBar()
                when (it) {
                    ApiCallStatus.SUCCESS -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (!com.joshtalks.joshskills.premium.core.PrefManager.getBoolValue(HAS_SEEN_POINTS_HISTORY_ANIMATION))
                                getOverlayView()
                        }
                    }
                    else -> {}
                }
            }
        )
    }

    suspend fun getOverlayView() {
        delay(1000)
        withContext(Dispatchers.Main) {
            var i = 0
            while (true) {
                val view = binding.recyclerView.getChildAt(i) ?: break
                if (view.id == R.id.root_view) {
                    val overlayItem = TooltipUtils.getOverlayItemFromView(view)
                    overlayItem?.let {
                        Log.d(TAG, "getOverlayView: $overlayItem")
                        val overlayImageView =
                            binding.overlayView.findViewById<ImageView>(R.id.profile_item_image)
                        val arrowImageView = view.findViewById<ImageView>(R.id.expand_unexpand_view)
                        val arrowPosition = IntArray(2)
                        arrowImageView.getLocationOnScreen(arrowPosition)
                        val arrowPoint = Point().apply {
                            x = arrowPosition[0]
                            y = arrowPosition[1]
                        }
                        overlayImageView.setOnClickListener {
                            binding.overlayView.visibility = View.INVISIBLE
                            isAnimationVisible = false
                            view.performClick()
                        }
                        setOverlayView(overlayItem, overlayImageView, arrowPoint, arrowImageView.width)
                    }
                    break
                }
                i++
            }
        }
    }

    fun setOverlayView(
        overlayItem: ItemOverlay,
        overlayImageView: ImageView,
        arrowPoint: Point,
        arrowWidth: Int
    ) {
        Log.d(TAG, "onViewBitmap: $overlayItem")
        val STATUS_BAR_HEIGHT = getStatusBarHeight()
        binding.overlayView.visibility = View.INVISIBLE
        binding.overlayView.setOnClickListener {
            binding.overlayView.visibility = View.INVISIBLE
            isAnimationVisible = false
        }
        val arrowView = binding.overlayView.findViewById<ImageView>(R.id.arrow_animation)
        val tooltipView = binding.overlayView.findViewById<JoshTooltip>(R.id.tooltip)
        val tapToDismissView =
            binding.overlayView.findViewById<AppCompatTextView>(R.id.label_tap_to_dismiss)
        val courseId = com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
            com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID, false,
            com.joshtalks.joshskills.premium.core.DEFAULT_COURSE_ID
        )
        overlayImageView.setImageBitmap(overlayItem.viewBitmap)
        overlayImageView.x = overlayItem.x.toFloat()
        overlayImageView.y = overlayItem.y.toFloat() - STATUS_BAR_HEIGHT
        overlayImageView.requestLayout()
        overlayImageView.post {
            arrowView.x =
                (arrowPoint.x + arrowWidth / 2.0).toFloat() - resources.getDimension(com.joshtalks.joshskills.R.dimen._40sdp)
            arrowView.y =
                overlayItem.y.toFloat() - STATUS_BAR_HEIGHT - resources.getDimension(com.joshtalks.joshskills.R.dimen._32sdp)
            arrowView.requestLayout()
            arrowView.visibility = View.VISIBLE
            binding.overlayView.visibility = View.VISIBLE
            tooltipView.setTooltipText(getString(R.string.tooltip_points_history_screen))

            slideInAnimation(tooltipView)
            com.joshtalks.joshskills.premium.core.PrefManager.put(HAS_SEEN_POINTS_HISTORY_ANIMATION, true)
            isAnimationVisible = true
            CoroutineScope(Dispatchers.IO).launch {
                showTapToDismiss(tapToDismissView)
            }
        }
    }

    fun getScreenHeightAndWidth(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels to metrics.widthPixels
    }

    fun slideInAnimation(tooltipView: JoshTooltip) {
        tooltipView.visibility = View.INVISIBLE
        val start = getScreenHeightAndWidth().second
        val mid = start * 0.2 * -1
        val end = tooltipView.x
        tooltipView.x = start.toFloat()
        tooltipView.requestLayout()
        tooltipView.visibility = View.VISIBLE
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

    @MainThread
    private suspend fun showTapToDismiss(labelTapToDismiss: AppCompatTextView) {
        withContext(Dispatchers.Main) {
            labelTapToDismiss.visibility = View.INVISIBLE
            delay(6500)
            labelTapToDismiss.visibility = View.VISIBLE
            labelTapToDismiss.startAnimation(
                AnimationUtils.loadAnimation(this@PointsHistoryActivity, R.anim.slide_up_dialog)
            )
        }
    }

    fun openPointsInfoTable() {
        MixPanelTracker.publishEvent(MixPanelEvent.HOW_TO_EARN_POINTS).push()
        startActivity(
            Intent(this, PointsInfoActivity::class.java).apply {
                putExtra(CONVERSATION_ID, intent.getStringExtra(CONVERSATION_ID))
            }
        )
    }

    fun showFreeTrialPaymentScreen() {
//        FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
//            this,
//            AppObjectController.getFirebaseRemoteConfig().getString(
//                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
//            ),
//            viewModel.pointsHistoryLiveData.value?.expiryDate?.time
//        )
        BuyPageActivity.startBuyPageActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            "POINTS_HISTORY"
        )
        // finish()
    }

    companion object {
        fun startPointHistory(
            context: Activity,
            mentorId: String? = null,
            conversationId: String? = null
        ) {
            val intent = Intent(context, PointsHistoryActivity::class.java)
            intent.putExtra(CONVERSATION_ID, conversationId)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            mentorId?.run {
                intent.putExtra(MENTOR_ID, mentorId.toString())
            }
            context.startActivity(intent)
        }
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (isAnimationVisible) {
            binding.overlayView.visibility = View.INVISIBLE
            isAnimationVisible = false
        } else
            super.onBackPressed()
    }

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).getTop()
        val titleBarHeight = contentViewTop - statusBarHeight
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }
}
