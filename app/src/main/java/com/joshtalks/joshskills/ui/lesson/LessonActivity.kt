package com.joshtalks.joshskills.ui.lesson

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textview.MaterialTextView
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.ApiCallStatus.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.AVAIL_COUPON_BANNER_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_COURSE_BANNER_COUPON_UNLOCKED_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_COURSE_BANNER_LESSON_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.COUPON_UNLOCK_LESSON_COUNT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.LESSON_COMPLETE_COUPON_DISCOUNT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.LESSON_SPEAKING_BB_TIP_CONTENT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.LESSON_SPEAKING_BB_TIP_HEADER
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics.lessonNo2Complete
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.extension.translationAnimationNew
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.core.videotranscoder.recyclerView
import com.joshtalks.joshskills.databinding.LessonActivityBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.chat.extra.FirstCallBottomSheet
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GRAMMAR_ANIMATION
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
import com.joshtalks.joshskills.ui.lesson.popup.PurchaseDialog
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFullScreenFragment
import com.joshtalks.joshskills.ui.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.UserRating
import com.joshtalks.joshskills.ui.lesson.vocabulary.VocabularyFragment
import com.joshtalks.joshskills.ui.online_test.GrammarAnimation
import com.joshtalks.joshskills.ui.online_test.GrammarOnlineTestFragment
import com.joshtalks.joshskills.ui.online_test.util.A2C1Impressions
import com.joshtalks.joshskills.ui.online_test.util.AnimateAtsOptionViewEvent
import com.joshtalks.joshskills.ui.online_test.vh.AtsOptionView
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.utils.L2_CLAIM_NOW_CLICKED
import com.joshtalks.joshskills.ui.special_practice.utils.L2_COUPON_UNLOCKED
import com.joshtalks.joshskills.ui.special_practice.utils.LESSON
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.constant.State
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.lesson_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val SPEAKING_POSITION = 0
const val GRAMMAR_POSITION = 1
const val TRANSLATION_POSITION = 2
const val VOCAB_POSITION = 3
const val READING_POSITION = 4
const val DEFAULT_SPOTLIGHT_DELAY_IN_MS = 1300L
const val INTRO_VIDEO_ID = "-1"
private const val TAG = "LessonActivity"
private val STORAGE_READING_REQUEST_CODE = 3457

class LessonActivity : CoreJoshActivity(), LessonActivityListener, GrammarAnimation {
    private val event = EventLiveData
    private lateinit var binding: LessonActivityBinding
    private val courseId = PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID)

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }

    var introVideoUrl: String? = null
    var lastVideoWatchedDuration = 0L
    var d2pIntroVideoWatchedDuration = 0L
    lateinit var titleView: TextView
    private var isDemo = false
    private var isLesssonCompleted = false
    private var testId = -1
    private var whatsappUrl = EMPTY
    private val compositeDisposable = CompositeDisposable()

    var lesson: LessonModel? = null // Do not use this var
    private lateinit var tabs: ViewGroup
    val arrayFragment = arrayListOf<Fragment>()
    var lessonIsNewGrammar = false
    var lessonNumber = -1
    var defaultSection = -1
    private var ruleIdLeftList = ArrayList<Int>()
    private var ruleCompletedList: ArrayList<Int>? = arrayListOf()
    private var totalRuleList: ArrayList<Int>? = arrayListOf()
    private var introVideoControl = false
    private var isWhatsappRemarketingActive = false
    private var isTwentyMinFtuCallActive = false
    private var getLessonId = -1
    private var isIntroVideoCmpleted = false
    private var isTranslationDisabled: Int = 1
    private lateinit var filePath: String
    private lateinit var videoDownPath: String
    private lateinit var outputFile: String
    private var openLessonCompletedScreen: Boolean = false
    private var isLessonPopUpFeatureOn: Boolean = false
    private lateinit var toolTipBalloon: Balloon

    private val adapter: LessonPagerAdapter by lazy {
        LessonPagerAdapter(
            supportFragmentManager,
            this.lifecycle,
            arrayFragment,
        )
    }

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoView.progress = progress
                binding.videoView.onResume()
            }
        }
    }

    var openLessonCompletedActivity: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data!!.hasExtra(IS_BATCH_CHANGED)) {
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(IS_BATCH_CHANGED, false)
                        putExtra(LAST_LESSON_INTERVAL, lesson?.interval)
                        putExtra(LAST_LESSON_STATUS, true)
                        putExtra(LESSON__CHAT_ID, lesson?.chatId)
                        putExtra(CHAT_ROOM_ID, lesson?.chatId)
                    }
                )
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.lesson_activity
        )
        binding.viewbinding = this
        initToolbar()
        event.observe(this) {
            when (it.what) {
                PERMISSION_FROM_READING -> requestStoragePermission(STORAGE_READING_REQUEST_CODE)
                OPEN_READING_SHARING_FULLSCREEN -> openReadingFullScreen()
                CLOSE_FULL_READING_FRAGMENT -> closeReadingFullScreen()
                CLOSE_INTEREST_ACTIVITY -> viewModel.checkPopupDisplay()
                SHOW_SCRATCH_CARD -> VoipPref.showScratchCard(this, VoipPref.getLastCallDurationInSec() * 1000L)
            }
        }

        PrefManager.put(LESSON_COMPLETE_SNACKBAR_TEXT_STRING, EMPTY, false)
        val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0
        getLessonId = lessonId
        if (lessonId == 0) {
            // InboxActivity.startInboxActivity(this)
            finish()
        }
        viewModel.isFreeTrail = PrefManager.getBoolValue(IS_FREE_TRIAL)
        isDemo = if (intent.hasExtra(IS_DEMO)) intent.getBooleanExtra(IS_DEMO, false) else false

        if (intent.hasExtra(IS_LESSON_COMPLETED)) {
            isLesssonCompleted = intent.getBooleanExtra(IS_LESSON_COMPLETED, false)
        } else {
            isLesssonCompleted = false
        }

        if (intent.hasExtra(LESSON_SECTION)) {
            defaultSection = intent.getIntExtra(LESSON_SECTION, 0)
        }

        if (intent.hasExtra(SHOULD_START_CALL)) {
            PrefManager.increaseCallCount()
            val callIntent = Intent(this, VoiceCallActivity::class.java)
            callIntent.apply {
                putExtra(INTENT_DATA_COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
                putExtra(INTENT_DATA_TOPIC_ID, "5")
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            }
            VoipPref.resetAutoCallCount()
            startActivity(callIntent)
        }

        whatsappUrl =
            if (intent.hasExtra(WHATSAPP_URL) && intent.getStringExtra(WHATSAPP_URL).isNullOrBlank()
                    .not()
            ) intent.getStringExtra(WHATSAPP_URL) ?: EMPTY else EMPTY
        testId = intent.getIntExtra(TEST_ID, -1)

        titleView = findViewById(R.id.text_message_title)
        if (isDemo)
            binding.buyCourseLl.visibility = View.VISIBLE
        else if (PrefManager.getBoolValue(IS_FREE_TRIAL))
            showBottomCouponBanner()
        setObservers()
        viewModel.getLesson(lessonId)
        viewModel.getQuestions(lessonId, isDemo)

        val helpIv: ImageView = findViewById(R.id.iv_help)
        helpIv.visibility = View.GONE
        if (PrefManager.getBoolValue(HAS_SEEN_QUIZ_VIDEO_TOOLTIP).not()) {
            binding.tooltipFrame.setOnClickListener { showVideoToolTip(false) }
            binding.overlayTooltipLayout.setOnClickListener { showVideoToolTip(false) }
            binding.tooltipTv.setOnClickListener { showVideoToolTip(false) }
        }
        viewModel.saveImpression(IMPRESSION_OPEN_SPEAKING_SCREEN)
        binding.imageViewClose.setOnClickListener {
            viewModel.saveImpression(SIV_VIDEO_CANCELED)
            closeVideoPopUpUi()
        }
        viewModel.lessonId.postValue(getLessonId)
    }

    fun getBottomBannerHeight(): Int {
        return if (binding.buyCourseBanner.isVisible) binding.buyCourseBanner.height else 0
    }

    private fun showBottomCouponBanner() {
        if (viewModel.abTestRepository.isVariantActive(VariantKeys.L2_LESSON_COMPLETE_ENABLED)) {
            binding.buyCourseBanner.visibility = View.VISIBLE
            viewModel.getCompletedLessonCount(courseId)
            viewModel.completedLessonCount.observe(this) { count ->
                count?.let {
                    val lessonCompletionCount =
                        AppObjectController.getFirebaseRemoteConfig().getLong(COUPON_UNLOCK_LESSON_COUNT).toInt()
                    (count >= lessonCompletionCount).let {
                        with(binding) {
                            buyCourseBannerTv.text = (if (it)
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(BUY_COURSE_BANNER_COUPON_UNLOCKED_TEXT)
                            else AppObjectController.getFirebaseRemoteConfig().getString(BUY_COURSE_BANNER_LESSON_TEXT))
                                .replace(
                                    "\$DISCOUNT\$",
                                    AppObjectController.getFirebaseRemoteConfig()
                                        .getLong(LESSON_COMPLETE_COUPON_DISCOUNT)
                                        .toString()
                                )
                            buyCourseBannerLessonProgressBar.isVisible = it.not()
                            buyCourseBannerLessonProgressTv.isVisible = it.not()
                            buyCourseBannerAvailBtn.isVisible = it
                            buyCourseBannerAvailBtn.text = getString(R.string.claim_now)
                            viewModel.saveImpression(L2_COUPON_UNLOCKED)
                            binding.buyCourseBannerAvailBtn.setOnClickListener {
                                viewModel.saveImpression(L2_CLAIM_NOW_CLICKED)
                                BuyPageActivity.startBuyPageActivity(
                                    this@LessonActivity,
                                    testId.toString(),
                                    "offer coupon banner",
                                    shouldAutoApplyCoupon = true,
                                    shouldAutoApplyFrom = LESSON
                                )
                            }
                            if (it.not()) {
                                buyCourseBannerLessonProgressBar.max = lessonCompletionCount
                                buyCourseBannerLessonProgressBar.progress = count
                                buyCourseBannerLessonProgressTv.text =
                                    getString(R.string.slash_2, count, lessonCompletionCount)
                            }
                        }
                    }
                }
            }
        }

        if (viewModel.abTestRepository.isVariantActive(VariantKeys.OTHER_SCREENS_BANNER_ENABLED)) {
            lifecycleScope.launch {
                viewModel.getMentorCoupon(testId)?.let { coupon ->
                    binding.buyCourseBanner.visibility = View.VISIBLE
                    binding.buyCourseBannerTv.text =
                        AppObjectController.getFirebaseRemoteConfig().getString(AVAIL_COUPON_BANNER_TEXT)
                            .replace("\$DISCOUNT\$", coupon.title)
                            .replace("\$CODE\$", coupon.couponCode)
                    binding.buyCourseBannerAvailBtn.visibility = View.VISIBLE
                    binding.buyCourseBannerAvailBtn.text = getString(R.string.avail_now)
                    binding.buyCourseBannerAvailBtn.setOnClickListener {
                        when (binding.lessonViewpager.currentItem) {
                            SPEAKING_POSITION -> GoalKeys.SPEAKING_SEC_BANNER_CLICKED
                            GRAMMAR_POSITION -> GoalKeys.GRAMMAR_SEC_BANNER_CLICKED
                            VOCAB_POSITION - isTranslationDisabled -> GoalKeys.VOCAB_SEC_BANNER_CLICKED
                            READING_POSITION - isTranslationDisabled -> GoalKeys.READING_SEC_BANNER_CLICKED
                            else -> null
                        }?.name?.let {
                            viewModel.postGoal(
                                it,
                                CampaignKeys.OFFER_BANNER_OTHER_SCREENS.name
                            )
                            viewModel.saveImpression(it)
                        }
                        BuyPageActivity.startBuyPageActivity(
                            this@LessonActivity,
                            testId.toString(),
                            "l2 complete banner",
                            coupon.couponCode
                        )
                    }
                }
            }
        }
    }

    private fun initToolbar() {
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }
        binding.toolbarContainer.findViewById<MaterialTextView>(R.id.btn_upgrade).apply {
            isVisible = PrefManager.getBoolValue(IS_FREE_TRIAL)
            setOnClickListener {
//                FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
//                    this@LessonActivity,
//                    AppObjectController.getFirebaseRemoteConfig().getString(
//                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
//                    )
//                )
                BuyPageActivity.startBuyPageActivity(
                    this@LessonActivity,
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                    ),
                    "LESSON_TOOLBAR_BTN"
                )
            }
        }
    }

    private fun requestStoragePermission(requestCode: Int) {
        PermissionUtils.storageReadAndWritePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        if (report.areAllPermissionsGranted()) {
                            if (requestCode == STORAGE_READING_REQUEST_CODE)
                                viewModel.permissionGranted()
                        } else if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                this@LessonActivity,
                                R.string.grant_storage_permission
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.isUserCallBlock()
        PrefManager.put(LESSON_ACTIVITY_VISIT_COUNT, PrefManager.getIntValue(LESSON_ACTIVITY_VISIT_COUNT).plus(1))
        subscribeRxBus()
    }

    private fun subscribeRxBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(AnimateAtsOptionViewEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    AtsOptionView(this, event.atsOptionView.choice).apply {
                        binding.rootView.addView(this)
                        this.text = event.atsOptionView.choice.text
                        this.x = event.fromLocation[0].toFloat()
                        this.y = event.fromLocation[1].toFloat() - event.height.toFloat()
                        val toLocation = IntArray(2)
                        event.atsOptionView.getLocationOnScreen(toLocation)
                        toLocation[1] = toLocation[1] - getStatusBarHeight()
                        this.translationAnimationNew(
                            toLocation,
                            event.atsOptionView,
                            event.optionLayout,
                            doOnAnimationEnd = {
                                binding.rootView.removeView(this)
                            }
                        )
                    }
                }
        )
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.installActivity(this)
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setObservers() {
        viewModel.abTestRepository.apply {
            isWhatsappRemarketingActive = isVariantActive(VariantKeys.WR_ENABLED)
            isTwentyMinFtuCallActive = isVariantActive(VariantKeys.TWENTY_MIN_ENABLED)
        }
        viewModel.apiStatus.observe(this) {
            when (it) {
                START -> {
                    binding.progressView.visibility = View.GONE
                }
                FAILED -> {
                    binding.progressView.visibility = View.GONE
                    AppObjectController.uiHandler.post {
                        showToast(getString(R.string.internet_not_available_msz))
                    }
                    finish()
                }
                SUCCESS -> {
                    binding.progressView.visibility = View.GONE
                }
                else -> {
                    binding.progressView.visibility = View.GONE
                }
            }
        }
        viewModel.lessonQuestionsLiveData.observe(
            this
        ) {
            viewModel.lessonLiveData.value?.let {
                titleView.text = getString(R.string.lesson_no, it.lessonNo)
                lessonNumber = it.lessonNo
                lessonIsNewGrammar = it.isNewGrammar
            }
            MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_OPENED)
                .addParam(ParamKeys.LESSON_ID, getLessonId)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                .push()
            viewModel.postGoal(GoalKeys.GRAMMAR_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
            if (lessonNumber == 1) {
                viewModel.postGoal(GoalKeys.LESSON1_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
            }

            if (lessonIsNewGrammar) {

                totalRuleList = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(ONLINE_TEST_LIST_OF_TOTAL_RULES),
                    object : TypeToken<ArrayList<Int>?>() {}.type
                )
                if (totalRuleList.isNullOrEmpty()) {
                    viewModel.getListOfRuleIds()
                } else {
                    ruleCompletedList = AppObjectController.gsonMapper.fromJson(
                        PrefManager.getStringValue(ONLINE_TEST_LIST_OF_COMPLETED_RULES),
                        object : TypeToken<ArrayList<Int>?>() {}.type
                    )
                    setUpNewGrammarLayouts(ruleCompletedList, totalRuleList)
                }
            } else {
                setUpTabLayout(lessonNumber, lessonIsNewGrammar)
                setTabCompletionStatus()
            }
        }

        viewModel.ruleListIds.observe(
            this
        ) { ruleIds ->
            if (ruleIds.totalRulesIds.isNullOrEmpty().not()) {
                PrefManager.put(
                    ONLINE_TEST_LIST_OF_TOTAL_RULES,
                    ruleIds.totalRulesIds.toString()
                )
                PrefManager.put(
                    ONLINE_TEST_LIST_OF_COMPLETED_RULES,
                    ruleIds.rulesCompletedIds.toString()
                )
                setUpNewGrammarLayouts(ruleIds.rulesCompletedIds, ruleIds.totalRulesIds)
            }
        }

        viewModel.updatedLessonResponseLiveData.observe(
            this
        ) {
            if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList?.get(0))
                    playSnackbarSound(this)
                    it.pointsList?.let { it1 ->
                        PrefManager.put(
                            LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                            it1.last(), false
                        )
                    }
                }
            }
        }

        viewModel.pointsSnackBarText.observe(
            this
        ) {
            if (it.pointsList.isNullOrEmpty().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!![0])
                PrefManager.put(
                    LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                    it.pointsList.last(),
                    false
                )
            }
        }

        viewModel.lessonSpotlightStateLiveData.observe(this) {
            // Show lesson Spotlight
            when (it) {
                LessonSpotlightState.LESSON_SPOTLIGHT -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_lesson_spotlight).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.SPEAKING_SPOTLIGHT -> {
//                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
//                    binding.spotlightTabSpeaking.visibility = View.VISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
//                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_speaking_spotlight).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.GRAMMAR_SPOTLIGHT_PART1 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.VISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_grammar_spotlight).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.VOCAB_SPOTLIGHT_PART1 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.VISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_vocab_spotlight_1).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.VOCAB_SPOTLIGHT_PART2 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.VISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_vocab_spotlight_2).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.VOCAB_SPOTLIGHT_PART3 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.VISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_vocab_spotlight_3).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.READING_SPOTLIGHT -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_reading_spotlight).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                LessonSpotlightState.GRAMMAR_SPOTLIGHT_PART2 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.VISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.setTooltipText(
                        resources.getText(R.string.label_grammar_spotlight).toString()
                    )
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.VISIBLE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.VISIBLE
                }

                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    lifecycleScope.launch (Dispatchers.Main){
                        Log.e("sagar", "setObservers1234: $introVideoControl $introVideoUrl")
                        if (introVideoControl) {
                            if (introVideoUrl.isNullOrBlank().not()) {
                                viewModel.saveIntroVideoFlowImpression(
                                    SPEAKING_TAB_CLICKED_FOR_FIRST_TIME
                                )
                                viewModel.showHideSpeakingFragmentCallButtons(1)
                                showIntroVideoUi()
                            }
                        }
                    }
                }
                else -> {
                    // Hide lesson Spotlight
                    binding.overlayLayout.visibility = View.GONE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.spotlightTabConvo.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.GONE
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
            }

        }

        viewModel.introVideoLiveDataForSpeakingSection.observe(this) {
            Log.e("sagar", "setObservers111: ${it.videoLink}" )
            introVideoUrl = it.videoLink
            if (introVideoUrl.isNullOrBlank().not()) {
                showIntroVideoUi()
            } else {
                showToast("Something went wrong")
            }
        }

        viewModel.howToSpeakLiveData.observe(this) {
            if (it == true) {
                if (introVideoUrl.isNullOrBlank().not()) {
                    showIntroVideoUi()
                } else {
                    showToast("Something went wrong")
                }
            }
        }

        videoEvent.observe(this) { event ->
            event.getContentIfNotHandledOrReturnNull()?.let {
                binding.apply {
                    spotlightTabGrammar.visibility = View.INVISIBLE
                    spotlightTabSpeaking.visibility = View.INVISIBLE
                    spotlightTabVocab.visibility = View.INVISIBLE
                    spotlightTabReading.visibility = View.INVISIBLE
                    spotlightCallBtn.visibility = View.GONE
                    viewModel.showHideSpeakingFragmentCallButtons(1)
                    videoPopup.visibility = View.VISIBLE
                    videoPopup.startAnimation(
                        AnimationUtils.loadAnimation(
                            root.context,
                            R.anim.fade_in
                        )
                    )
                    videoView.seekToStart()
                    videoView.apply {
                        setUrl(it.video_url)
                        //setVideoId(it.id)
                        if (it.video_height != 0 && it.video_width != 0) {
                            (layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                                (it.video_width / it.video_height).toString()
                        }
                        onStart()
                        setFullScreenListener {
                            val currentVideoProgressPosition = binding.videoView.progress
                            openVideoPlayerActivity.launch(
                                VideoPlayerActivity.getActivityIntent(
                                    root.context,
                                    "",
                                    it.id,
                                    it.video_url,
                                    currentVideoProgressPosition,
                                    conversationId = getConversationId()
                                )
                            )
                        }
                        setPlayerCompletionCallback {
                            if (it.id != INTRO_VIDEO_ID) {
                                PrefManager.appendToSet(
                                    LAST_SEEN_VIDEO_ID,
                                    it.id,
                                    false
                                )
                                A2C1Impressions.saveImpression(A2C1Impressions.Impressions.RULE_VIDEO_COMPLETED)
                            }
                        }
                        seekToStart()
                        downloadStreamPlay()
                        outlineProvider = object : ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, 15f)
                            }
                        }
                        clipToOutline = true
                    }
                }
            }
        }
        viewModel.filePath.observe(this) {
            filePath = it
        }
        viewModel.videoDownPath.observe(this) {
            videoDownPath = it
        }
        viewModel.outputFile.observe(this) {
            outputFile = it
        }
        viewModel.coursePopupData.observe(this) {
            if (it != null) {
                PurchaseDialog
                    .newInstance(it)
                    .apply {
                        if (openLessonCompletedScreen) {
                            setOnDismissListener {
                                openLessonCompleteScreen(
                                    viewModel.lessonLiveData.value ?: lesson ?: return@setOnDismissListener
                                )
                            }
                        }
                        show(supportFragmentManager, PurchaseDialog::class.simpleName)
                    }
                if (it.couponCode != null && it.couponExpiryTime != null)
                    PrefManager.put(COUPON_EXPIRY_TIME, it.couponExpiryTime.time)
            }
        }
        viewModel.lessonCompletePopUpClick.observe(this) {
            binding.lessonTabLayout.selectTab(binding.lessonTabLayout.getTabAt(it))
        }
    }

    private fun hideSpotlight() {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
    }

    private fun showLessonSpotlight() {
        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.LESSON_SPOTLIGHT)
        PrefManager.put(HAS_SEEN_LESSON_SPOTLIGHT, true)
    }

    fun startOnlineExamTest(v:View) {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        viewModel.grammarSpotlightClickLiveData.postValue(Unit)
    }

    fun callPracticePartner() {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        viewModel.speakingSpotlightClickLiveData.postValue(Unit)
        if (introVideoControl) closeVideoPopUpUi()
    }

    fun callPracticePartner(v:View) {
        callPracticePartner()
    }

    private fun openReadingFullScreen() {
        binding.containerReading.visibility = View.VISIBLE
        supportFragmentManager.commit {
            val fragment = ReadingFullScreenFragment.newInstance(getLessonId)
            replace(R.id.container_reading, fragment, ReadingFullScreenFragment::class.java.simpleName)
        }
    }

    private fun setUpNewGrammarLayouts(
        rulesCompletedIds: ArrayList<Int>?,
        totalRulesIds: ArrayList<Int>?
    ) {
        var isTestCompleted = false
        if (rulesCompletedIds.isNullOrEmpty().not()) {
            totalRulesIds?.removeAll(rulesCompletedIds!!)
            ruleIdLeftList = totalRulesIds ?: ArrayList<Int>()
            if (ruleIdLeftList.isEmpty()) {
                isTestCompleted = true
            }
        }
        setUpTabLayout(lessonNumber, lessonIsNewGrammar, isTestCompleted)
        setTabCompletionStatus()
    }

    override fun onNextTabCall(currentTabNumber: Int) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.lessonLiveData.value?.let { lesson ->
                    var lessonCompleted = lesson.grammarStatus == LESSON_STATUS.CO &&
                            lesson.vocabStatus == LESSON_STATUS.CO &&
                            lesson.readingStatus == LESSON_STATUS.CO &&
                            lesson.speakingStatus == LESSON_STATUS.CO

                    if (lesson.isNewGrammar && PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)) {
                        lessonCompleted = lessonCompleted &&
                                lesson.translationStatus == LESSON_STATUS.CO
                    }
                    if (lessonCompleted) {
                        PrefManager.put(LESSON_COMPLETED_FOR_NOTIFICATION, true)
                        if (lesson.status != LESSON_STATUS.CO) {
                            MarketingAnalytics.logLessonCompletedEvent(lesson.lessonNo, lesson.id)
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                MarketingAnalytics.logLessonCompletedEventForFreeTrial(lesson.lessonNo)
                            }
                        }
                        lesson.status = LESSON_STATUS.CO
                        viewModel.updateLesson(lesson)
                        openLessonCompleteScreen(lesson)
                    } else {
                        AppObjectController.uiHandler.post {
                            openIncompleteTab(currentTabNumber)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showLessonCompleteCard() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.lessonLiveData.value?.let { lesson ->
                    var lessonCompleted = lesson.grammarStatus == LESSON_STATUS.CO &&
                            lesson.vocabStatus == LESSON_STATUS.CO &&
                            lesson.readingStatus == LESSON_STATUS.CO &&
                            lesson.speakingStatus == LESSON_STATUS.CO

                    if (lesson.isNewGrammar && PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)) {
                        lessonCompleted = lessonCompleted &&
                                lesson.translationStatus == LESSON_STATUS.CO
                    }
                    if (lessonCompleted) {
                        if (lesson.lessonNo == 2) {
                            lessonNo2Complete()
                        }
                        if (lesson.status != LESSON_STATUS.CO) {
                            MarketingAnalytics.logLessonCompletedEvent(lesson.lessonNo, lesson.id)
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                MarketingAnalytics.logLessonCompletedEventForFreeTrial(lesson.lessonNo)
                            }
                        }
                        lesson.status = LESSON_STATUS.CO
                        viewModel.updateLesson(lesson)
                        if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                            openLessonCompletedScreen = true
                            viewModel.getCoursePopupData(PurchasePopupType.LESSON_COMPLETED)
                        } else {
                            AppObjectController.uiHandler.post {
                                openLessonCompleteScreen(lesson)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onQuestionStatusUpdate(
        status: QUESTION_STATUS,
        questionId: String?,
        isVideoPercentComplete: Boolean,
        quizCorrectQuestionIds: ArrayList<Int>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.updateQuestionStatus(
                status,
                questionId,
                isVideoPercentComplete,
                quizCorrectQuestionIds
            )
        }
        AppObjectController.uiHandler.post {
            setTabCompletionStatus()
        }
    }

    override fun onLessonUpdate() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                viewModel.updateLessonStatus()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun showIntroVideo() {
        Log.e("sagar", "showIntroVideo: ", )
        introVideoControl = true
        setUpVideoProgressListener()
        viewModel.getVideoData()
    }

    override fun introVideoCmplt() {
        isIntroVideoCmpleted = false
    }

    override fun showVideoToolTip(
        shouldShow: Boolean,
        wrongAnswerHeading: String?,
        wrongAnswerSubHeading: String?,
        wrongAnswerText: String?,
        wrongAnswerDescription: String?,
        videoClickListener: (() -> Unit)?
    ) {
        if (shouldShow.not()) PrefManager.put(HAS_SEEN_QUIZ_VIDEO_TOOLTIP, true)
        with(binding)
        {
            tooltipFrame.isVisible = shouldShow
            videoBtnTooltip.isVisible = shouldShow
            overlayTooltipLayout.isVisible = shouldShow
            videoIvBtn.setOnClickListener {
                showVideoToolTip(false)
                if (videoClickListener != null) {
                    videoClickListener()
                }
            }
            wrongAnswerTitle.isVisible = wrongAnswerHeading.isNullOrEmpty().not()
            explanationTitle.isVisible = wrongAnswerSubHeading.isNullOrEmpty().not()
            wrongAnswerDesc.isVisible = wrongAnswerText.isNullOrEmpty().not()
            explanationText.isVisible = wrongAnswerDescription.isNullOrEmpty().not()
            wrongAnswerTitle.text = wrongAnswerHeading
            explanationTitle.text = wrongAnswerSubHeading
            wrongAnswerDesc.text = wrongAnswerText
            explanationText.text = wrongAnswerDescription
        }
    }

    override fun onSectionStatusUpdate(tabPosition: Int, isSectionCompleted: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.lessonLiveData.value?.let { lesson ->
                val status = if (isSectionCompleted) LESSON_STATUS.CO else LESSON_STATUS.NO
                when (tabPosition) {
                    SPEAKING_POSITION -> {
                        if (lesson.speakingStatus != LESSON_STATUS.CO && status == LESSON_STATUS.CO) {
                            MarketingAnalytics.logSpeakingSectionCompleted()
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                MarketingAnalytics.logSpeakingSectionCompletedForFreeTrial()
                            }
                        }
                        lesson.speakingStatus = status
                    }
                    GRAMMAR_POSITION -> {
                        lesson.grammarStatus = status
                    }
                    VOCAB_POSITION -> lesson.vocabStatus = status
                    READING_POSITION -> lesson.readingStatus = status
                    TRANSLATION_POSITION -> lesson.translationStatus = status
                }
                viewModel.updateSectionStatus(lesson.id, status, tabPosition)
            }
        }
        AppObjectController.uiHandler.post {
            setTabCompletionStatus()
        }
    }

    private fun setUpTabLayout(
        lessonNo: Int,
        lessonIsNewGrammar: Boolean,
        isTestCompleted: Boolean = false
    ) {
        binding.lessonTabLayout.removeAllTabs()
        isTranslationDisabled = 1
        arrayFragment.add(
            SPEAKING_POSITION,
            SpeakingPractiseFragment.newInstance()
        )
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT) && lessonIsNewGrammar &&
            PrefManager.hasKey(IS_A2_C1_RETENTION_ENABLED) &&
            PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED) &&
            PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID
        ) {
            arrayFragment.add(GRAMMAR_POSITION, GrammarFragment.getInstance())
            arrayFragment.add(TRANSLATION_POSITION, GrammarOnlineTestFragment.getInstance(lessonNo))
            A2C1Impressions.saveImpression(A2C1Impressions.Impressions.START_LESSON_CLICKED)
            isTranslationDisabled = 0
        } else if (lessonIsNewGrammar) {
            arrayFragment.add(GRAMMAR_POSITION, GrammarOnlineTestFragment.getInstance(lessonNo))
        } else {
            arrayFragment.add(GRAMMAR_POSITION, GrammarFragment.getInstance())
        }
        arrayFragment.add(VOCAB_POSITION - isTranslationDisabled, VocabularyFragment.getInstance())
        arrayFragment.add(
            READING_POSITION - isTranslationDisabled,
            ReadingFragmentWithoutFeedback.getInstance()
        )
        binding.lessonViewpager.adapter = adapter
        binding.lessonViewpager.requestTransparentRegion(binding.lessonViewpager)
        binding.lessonViewpager.offscreenPageLimit = arrayFragment.size
        binding.lessonViewpager.recyclerView.enforceSingleScrollDirection()
        tabs = binding.lessonTabLayout.getChildAt(0) as ViewGroup
        for (i in 0 until tabs.childCount) {
            val tab = tabs.getChildAt(i)
            val layoutParams = tab.layoutParams as LinearLayout.LayoutParams
            layoutParams.weight = 0f
            //     layoutParams.marginEnd = Utils.dpToPx(2)
            //  layoutParams.marginStart = Utils.dpToPx(2)
        }
        binding.lessonTabLayout.requestLayout()

        TabLayoutMediator(
            binding.lessonTabLayout,
            binding.lessonViewpager
        ) { tab, position ->
            tab.setCustomView(R.layout.capsule_tab_layout_view)
            when (position) {
                SPEAKING_POSITION -> {
//                    if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT).not()) {
//                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2)
//                    }
                    binding.welcomeContainer.visibility = View.GONE
                    dismissTooltipButton()
                    setSelectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SPEAKING_TITLE)
                }
                GRAMMAR_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.GRAMMAR_TITLE)
                }
                VOCAB_POSITION - isTranslationDisabled -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.VOCABULARY_TITLE)
                }
                READING_POSITION - isTranslationDisabled -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.READING_TITLE)
                }
                TRANSLATION_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        getString(R.string.translation)
                }
            }
        }.attach()


        binding.lessonTabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                dismissTooltipButton()
                binding.welcomeContainer.visibility = View.GONE
                setSelectedColor(tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                dismissTooltipButton()
                binding.welcomeContainer.visibility = View.GONE
                setSelectedColor(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                dismissTooltipButton()
                binding.welcomeContainer.visibility = View.GONE
                setUnselectedColor(tab)
            }
        })

        Handler().postDelayed(
            {
                if (defaultSection != -1) {
                    binding.lessonViewpager.currentItem =
                        if (defaultSection == SPEAKING_POSITION || defaultSection == GRAMMAR_POSITION) defaultSection else defaultSection - isTranslationDisabled
                } else {
                    openIncompleteTab(arrayFragment.size - 1)
                }
            },
            50
        )
    }

    fun showBuyCourseTooltip(tabPosition: Int) {
        if (binding.buyCourseBanner.isVisible) return
        when (tabPosition) {
            SPEAKING_POSITION -> return
            GRAMMAR_POSITION -> if (lessonIsNewGrammar && PrefManager.getBoolValue(HAS_SEEN_GRAMMAR_ANIMATION)
                    .not()
            ) return
            VOCAB_POSITION - isTranslationDisabled -> if (PrefManager.getBoolValue(HAS_SEEN_VOCAB_SCREEN)
                    .not()
            ) return
            READING_POSITION - isTranslationDisabled -> if (PrefManager.getBoolValue(HAS_SEEN_READING_SCREEN)
                    .not()
            ) return
        }
        val key = when (tabPosition) {
            GRAMMAR_POSITION -> FirebaseRemoteConfigKey.BUY_COURSE_GRAMMAR_TOOLTIP
            VOCAB_POSITION - isTranslationDisabled -> FirebaseRemoteConfigKey.BUY_COURSE_VOCABULARY_TOOLTIP
            READING_POSITION - isTranslationDisabled -> FirebaseRemoteConfigKey.BUY_COURSE_READING_TOOLTIP
            else -> ""
        }
        val text = AppObjectController.getFirebaseRemoteConfig().getString(key.plus(courseId))
        if (text.isBlank()) return
        try {
            val balloon = Balloon.Builder(this)
                .setLayout(R.layout.layout_bb_tip)
                .setHeight(BalloonSizeSpec.WRAP)
                .setIsVisibleArrow(true)
                .setBackgroundColorResource(R.color.surface_tip)
                .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                .setWidthRatio(0.85f)
                .setDismissWhenTouchOutside(true)
                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                .setLifecycleOwner(this)
                .setDismissWhenClicked(true)
                .setAutoDismissDuration(4000L)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//            .setPreferenceName(key)
//            .setShowCounts(3)
                .build()
            val textView = balloon.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
            textView.text = text
            balloon.showAlignBottom(binding.toolbarContainer.findViewById<MaterialTextView>(R.id.btn_upgrade))
        } catch (ex: Exception) {
            Log.d(TAG, "showBuyCourseTooltip: ${ex.message}")
        }
    }

    private fun isOnlineTestCompleted(): Boolean {
        if (ruleCompletedList.isNullOrEmpty()) {
            return false
        } else return ruleIdLeftList.isNullOrEmpty()
    }

    private fun openIncompleteTab(currentTabNumber: Int) {
        var nextTabIndex = currentTabNumber + 1
        while (nextTabIndex != currentTabNumber) {
            if (nextTabIndex == arrayFragment.size) {
                nextTabIndex = 0
            } else {
                viewModel.lessonLiveData.value?.let { lesson ->
                    when (nextTabIndex) {
                        SPEAKING_POSITION ->
                            if (lesson.speakingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem =
                                    SPEAKING_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        GRAMMAR_POSITION ->
                            if (lesson.grammarStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = GRAMMAR_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        VOCAB_POSITION - isTranslationDisabled ->
                            if (lesson.vocabStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem =
                                    VOCAB_POSITION - isTranslationDisabled
                                return
                            } else {
                                nextTabIndex++
                            }
                        READING_POSITION - isTranslationDisabled ->
                            if (lesson.readingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem =
                                    READING_POSITION - isTranslationDisabled
                                return
                            } else {
                                nextTabIndex++
                            }
                        TRANSLATION_POSITION ->
                            if (lesson.translationStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = TRANSLATION_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        else -> {
                            binding.lessonViewpager.currentItem = arrayFragment.size - 1
                            return
                        }
                    }
                }
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        when(requestCode) {
//            CALLING_ACTIVITY_REQUEST_CODE -> {
//                if(resultCode == RESULT_OK) {
//                    val duration = data?.getLongExtra(CALL_DURATION, 0L)
//                    if (duration != null && duration < 5 * 60L) {
//                        startActivity(Intent(this, AutoCallActivity::class.java))
//                    }
//                } else {
//                    Log.d(TAG, "onActivityResult: $requestCode")
//                }
//            }
//            else -> {
//                Log.d(TAG, "onActivityResult: $requestCode")
//            }
//        }
//        super.onActivityResult(requestCode, resultCode, data)
//    }

    private fun setTabCompletionStatus() {
        try {
            viewModel.lessonLiveData.value?.let { lesson ->
                if (lesson.lessonNo == 2) {
                    lessonNo2Complete()
                }
                if (lesson.lessonNo >= 2) {
                    PrefManager.put(LESSON_TWO_OPENED, true)
                }
                setTabCompletionStatus(
                    tabs.getChildAt(SPEAKING_POSITION),
                    lesson.speakingStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(GRAMMAR_POSITION),
                    lesson.grammarStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(TRANSLATION_POSITION),
                    lesson.translationStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(VOCAB_POSITION - isTranslationDisabled),
                    lesson.vocabStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(READING_POSITION - isTranslationDisabled),
                    lesson.readingStatus == LESSON_STATUS.CO
                )
            }
            if (isLesssonCompleted.not()) {
                showLessonCompleteCard()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    private fun setTabCompletionStatus(tab: View?, isSectionCompleted: Boolean) {
        tab?.let {
            if (isSectionCompleted) {
                it.findViewById<ImageView>(R.id.tab_iv).visibility = View.VISIBLE
            } else {
                it.findViewById<ImageView>(R.id.tab_iv).visibility = View.GONE
            }
        }
    }

    private fun setSelectedColor(tab: TabLayout.Tab?) {
        tab?.let {
            if (PrefManager.getBoolValue(IS_FREE_TRIAL))
                showBuyCourseTooltip(tab.position)
            val color = when (tab.position) {
                SPEAKING_POSITION -> R.color.decorative_four
                GRAMMAR_POSITION -> R.color.decorative_one
                VOCAB_POSITION -> R.color.external_whatsapp
                READING_POSITION -> R.color.primary_500
                TRANSLATION_POSITION -> R.color.accent_800
                else -> R.color.decorative_four
            }
            tab.view.findViewById<AppCompatTextView>(R.id.title_tv)
                ?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        setTextAppearance(R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                    else
                        setTextAppearance(this.context, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                    setTextColor(
                        ContextCompat.getColor(
                            this@LessonActivity,
                            color
                        )
                    )
                }
            binding.lessonTabLayout.setSelectedTabIndicatorColor(
                ContextCompat.getColor(
                    this,
                    color
                )
            )
            when (tab.position) {
                SPEAKING_POSITION -> {
                    viewModel.saveImpression(IMPRESSION_OPEN_SPEAKING_SCREEN)
                    PrefManager.put(IS_SPEAKING_SCREEN_CLICKED, true)
                    viewModel.postGoal(GoalKeys.SPEAKING_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                GRAMMAR_POSITION -> {
                    dismissTooltipButton()
                    binding.welcomeContainer.visibility = View.GONE
                    viewModel.saveImpression(IMPRESSION_OPEN_GRAMMAR_SCREEN)
                    MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                VOCAB_POSITION - isTranslationDisabled -> {
                    dismissTooltipButton()
                    binding.welcomeContainer.visibility = View.GONE
                    viewModel.saveImpression(IMPRESSION_OPEN_VOCABULARY_SCREEN)
                    viewModel.postGoal(GoalKeys.VOCABULARY_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.VOCABULARY_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                READING_POSITION - isTranslationDisabled -> {
                    dismissTooltipButton()
                    binding.welcomeContainer.visibility = View.GONE
                    viewModel.saveImpression(IMPRESSION_OPEN_READING_SCREEN)
                    viewModel.postGoal(GoalKeys.READING_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.READING_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
            }
        }

        if (lessonNumber == 1 && PrefManager.getBoolValue(IS_FREE_TRIAL)){
            try {
                viewModel.speakingTopicLiveData.observe(this){ response ->
                    Log.e("SAGAR", "setSelectedColor: 3${viewModel.abTestRepository.isVariantActive(VariantKeys.SPEAKING_TOOLTIP_V2_ENABLED)} ${introVideoControl}" )
                    if (!PrefManager.getBoolValue(HAS_SEEN_SPEAKING_BB_TIP_SHOW) && (tab?.position == 0 || tab?.position == -1)) {
                        Log.e("sagar", "setSelectedColor: " )
                        if (response == null) {
                            showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
                        }else {
                            if (viewModel.abTestRepository.isVariantActive(VariantKeys.SPEAKING_TOOLTIP_V2_ENABLED)) {
                                PrefManager.put(HAS_SEEN_SPEAKING_BB_TIP_SHOW, true)
                                viewModel.saveImpression(SPEAKING_TOOLTIP1)
                                Log.e("sagar", "setSelectedColor: 1")
                                binding.welcomeContainer.visibility = View.VISIBLE
                                toolTipBalloon = Balloon.Builder(this)
                                    .setLayout(R.layout.layout_speaking_button_tooltip)
                                    .setHeight(BalloonSizeSpec.WRAP)
                                    .setIsVisibleArrow(true)
                                    .setBackgroundColorResource(R.color.surface_tip)
                                    .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                                    .setWidthRatio(0.85f)
                                    .setDismissWhenTouchOutside(false)
                                    .setArrowPosition(0.2f)
                                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                                    .setLifecycleOwner(this)
                                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR).build()
                                val textViewTitle = toolTipBalloon.getContentView().findViewById<MaterialTextView>(R.id.title)
                                textViewTitle.text =
                                    AppObjectController.getFirebaseRemoteConfig().getString(LESSON_SPEAKING_BB_TIP_HEADER)
                                val textViewSubHeadingText =
                                    toolTipBalloon.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
                                textViewSubHeadingText.text = AppObjectController.getFirebaseRemoteConfig()
                                    .getString(LESSON_SPEAKING_BB_TIP_CONTENT.plus(courseId))
                                tab.view.let { toolTipBalloon.showAlignBottom(it) }

                                binding.welcomeContainer.setOnClickListener {
                                    Log.e("sagar", "setSelectedColor: 99",)
                                    binding.welcomeContainer.visibility = View.GONE
                                    dismissTooltipButton()
                                    PrefManager.put(HAS_SEEN_SPEAKING_BB_TIP_SHOW, true)
                                    viewModel.speakingTooltipLiveData.postValue(response)
                                }
                            } else {
                                Log.e("sagar", "setSelectedColor: 2")
                                //TODO here we have to add one more condition if we have to show by default tooltip
                                if (introVideoControl && !PrefManager.getBoolValue(HAS_SEEN_SPEAKING_VIDEO) && courseId == "151"){
                                    lifecycleScope.launch(Dispatchers.Main) {
                                       // delay(200)
                                        if (introVideoUrl.isNullOrBlank().not()) {
                                            Log.e("sagar", "setObservers: $introVideoControl $introVideoUrl")
                                            PrefManager.put(HAS_SEEN_SPEAKING_VIDEO, true)
                                            viewModel.saveIntroVideoFlowImpression(
                                                SPEAKING_TAB_CLICKED_FOR_FIRST_TIME
                                            )
                                            viewModel.saveIntroVideoFlowImpression(SIV_AUTOPLAYEDD)
                                            viewModel.showHideSpeakingFragmentCallButtons(1)
                                            showIntroVideoUi()
                                        }
                                    }
                                } else {
                                    if (!PrefManager.getBoolValue(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL) && (viewModel.lessonLiveData.value?.speakingStatus != LESSON_STATUS.CO) && !introVideoControl) {
                                        PrefManager.put(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL, true)
                                        binding.overlayLayout.visibility = View.VISIBLE
                                        binding.spotlightTabGrammar.visibility = View.INVISIBLE
                                        binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                                        binding.spotlightTabVocab.visibility = View.INVISIBLE
                                        binding.spotlightTabReading.visibility = View.INVISIBLE
                                        binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                                        binding.spotlightStartGrammarTest.visibility = View.GONE
                                        binding.spotlightCallBtn.visibility = View.VISIBLE
                                        binding.arrowAnimation.visibility = View.VISIBLE
                                        viewModel.speakingTopicLiveData.value?.speakingToolTipText?.let { text ->
                                            if (text.isBlank()) hideSpotlight()
                                            else {
                                                binding.lessonSpotlightTooltip.setTooltipText(text)
                                                binding.lessonSpotlightTooltip.post {
                                                    slideInAnimation(binding.lessonSpotlightTooltip)
                                                }
                                            }
                                        } ?: run {
                                            hideSpotlight()
                                        }
                                    }
                                }
                            }
                        }
                        // TODO else if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_BB_TIP_SHOW) && !PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT) && (tab?.position == 0 || tab?.position == -1)){
                    }else if (!PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT) && (tab?.position == 0 || tab?.position == -1)){
                        Log.e("sagar", "setSelectedColor: 3" )
                        viewModel.speakingTooltipLiveData.postValue(response)
                    }
                }
            } catch (ex: Exception) {
                Log.d("sagar", "showBuyCourseTooltip: ${ex.message}")
            }
        }
    }

    private fun dismissTooltipButton(){
        if (this::toolTipBalloon.isInitialized && toolTipBalloon.isShowing){
            toolTipBalloon.dismiss()
        }
    }

//    private fun showSpeakingSpotlight() {
//        if (lessonNumber == 1) {
//            viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2)
//            PrefManager.put(HAS_SEEN_SPEAKING_SPOTLIGHT, true)
//        }
//    }

    private fun setUnselectedColor(tab: TabLayout.Tab?) {
        tab?.view?.findViewById<AppCompatTextView>(R.id.title_tv)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                setTextAppearance(R.style.TextAppearance_JoshTypography_CaptionRegular)
            else
                setTextAppearance(this@LessonActivity, R.style.TextAppearance_JoshTypography_CaptionRegular)
            setTextColor(ContextCompat.getColor(this@LessonActivity, R.color.text_subdued))
        }
    }

    private fun openLessonCompleteScreen(lesson: LessonModel) {
        if (PrefManager.getBoolValue("DelayLessonCompletedActivity")) {
            PrefManager.put("OpenLessonCompletedActivity", true)
            PrefManager.putPrefObject("lessonObject", lesson)
        } else {
            this.lesson = lesson
            openLessonCompletedActivity.launch(
                LessonCompletedActivity.getActivityIntent(
                    this,
                    lesson
                )
            )
        }
    }

    fun buyCourse(v:View) {
        if (testId != -1) {
            PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString())
        }
    }

    fun openWhatsapp(v:View) {
        if (whatsappUrl.isNullOrBlank().not()) {
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse(whatsappUrl)
            whatsappIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(whatsappIntent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("reopen", false) == true) {
            return
        }
        intent?.let {
            val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0
            viewModel.getLesson(lessonId)
            viewModel.getQuestions(lessonId, isDemo)
        }
    }

    override fun onPause() {
        if (introVideoControl) binding.videoView.onPause()
        super.onPause()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
            isLessonPopUpFeatureOn = AppObjectController.getFirebaseRemoteConfig().getBoolean(
                FirebaseRemoteConfigKey.IS_LESSON_COMPLETE_POPUP_ENABLE
            )
        }
        when {
            binding.itemOverlay.isVisible -> binding.itemOverlay.isVisible = false
            binding.overlayTooltipLayout.isVisible -> showVideoToolTip(false)
            binding.videoPopup.isVisible -> closeVideoPopUpUi()
            binding.overlayLayout.isVisible -> hideSpotlight()
            viewModel.isSpeakingButtonTooltipShown.get() == true ->{
                viewModel.speakingLiveData.postValue(true)
                viewModel.isSpeakingButtonTooltipShown.set(false)
            }
            binding.containerReading.isVisible -> {
                supportFragmentManager.beginTransaction().remove(ReadingFullScreenFragment()).commit()
                viewModel.closeVideoView()
                closeReadingFullScreen()
                viewModel.showVideoView()
            }
            VoipPref.preferenceManager.getBoolean(IS_FIRST_CALL, true) && PrefManager.getBoolValue(IS_FREE_TRIAL) -> {
                if (getVoipState() == State.IDLE &&
                    PrefManager.getIntValue(FT_CALLS_LEFT) == 15 &&
                    PrefManager.getBoolValue(IS_COURSE_BOUGHT).not()
                )
                    FirstCallBottomSheet.showDialog(supportFragmentManager)
            }
            isLesssonCompleted.not() && PrefManager.getBoolValue(IS_FREE_TRIAL) && isLessonPopUpFeatureOn -> {
                // if lesson is not completed and FT user presses back, we want to show a prompt
                CompleteLessonBottomSheetFragment.newInstance()
                    .show(supportFragmentManager, "LessonCompleteDialog")
            }
            else -> {
                val resultIntent = Intent()
                viewModel.lessonLiveData.value?.let {
                    resultIntent.putExtra(CHAT_ROOM_ID, it.chatId)
                    resultIntent.putExtra(LAST_LESSON_INTERVAL, it.interval)
                    resultIntent.putExtra(LAST_LESSON_STATUS, it.status?.name)
                    resultIntent.putExtra(LESSON_NUMBER, it.lessonNo)
                }
                setResult(RESULT_OK, resultIntent)
                this@LessonActivity.finish()
            }
        }
    }

    override fun onVisibleScreen() {

    }

    override fun onInVisibleScreen() {

    }

    companion object {
        const val LESSON_ID = "lesson_id"
        const val IS_DEMO = "is_demo"
        const val IS_LESSON_COMPLETED = "is_lesson_completed"
        private const val WHATSAPP_URL = "whatsapp_url"
        private const val TEST_ID = "test_id"
        const val LAST_LESSON_STATUS = "last_lesson_status"
        const val LESSON_SECTION = "lesson_section"
        const val SHOULD_START_CALL = "should_start_call"
        val videoEvent: MutableLiveData<Event<VideoModel>> = MutableLiveData()

        fun getActivityIntent(
            context: Context,
            lessonId: Int,
            isDemo: Boolean = false,
            whatsappUrl: String? = null,
            testId: Int? = null,
            conversationId: String? = null,
            isLessonCompleted: Boolean = false,
            shouldStartCall: Boolean = false
        ) = Intent(context, LessonActivity::class.java).apply {
            // TODO: Pass Free Trail Status
            putExtra(LESSON_ID, lessonId)
            putExtra(IS_DEMO, isDemo)
            putExtra(IS_LESSON_COMPLETED, isLessonCompleted)
            putExtra(CONVERSATION_ID, conversationId)
            if (isDemo) {
                putExtra(WHATSAPP_URL, whatsappUrl)
                putExtra(TEST_ID, testId)
            }
            if (shouldStartCall)
                putExtra(SHOULD_START_CALL, true)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
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

    fun getScreenHeightAndWidth(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels to metrics.widthPixels
    }

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).top
        val titleBarHeight = contentViewTop - statusBarHeight
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }

    override fun showGrammarAnimation(overlayItem: ItemOverlay) {
        try {
            binding.itemOverlay.visibility = View.INVISIBLE
            binding.itemOverlay.setOnClickListener(null)
            val OFFSET = getStatusBarHeight()
            val itemImageView = binding.itemOverlay.findViewById<ImageView>(R.id.main_item_imageview)
            val arrowView =
                binding.itemOverlay.findViewById<LottieAnimationView>(R.id.arrow_animation_lesson)
            val tooltipView = binding.itemOverlay.findViewById<JoshTooltip>(R.id.tooltip)
            tooltipView.visibility = View.INVISIBLE
            itemImageView.visibility = View.INVISIBLE
            arrowView.visibility = View.INVISIBLE
            itemImageView.setImageBitmap(overlayItem.viewBitmap)
            arrowView.y = overlayItem.y.toFloat() - OFFSET - resources.getDimension(R.dimen._32sdp)
            itemImageView.x = overlayItem.x.toFloat()
            itemImageView.y = overlayItem.y.toFloat() - OFFSET
            itemImageView.setOnClickListener {
                binding.itemOverlay.visibility = View.INVISIBLE
                viewModel.eventLiveData.postValue(Event(true))
            }
            itemImageView.requestLayout()
            itemImageView.post {
                arrowView.x =
                    (itemImageView.x + itemImageView.width / 2.0).toFloat() - resources.getDimension(R.dimen._40sdp)
                arrowView.requestLayout()
            }
            arrowView.requestLayout()
            arrowView.post {
                tooltipView.visibility = View.INVISIBLE
                tooltipView.y = arrowView.y - resources.getDimension(R.dimen._60sdp) - OFFSET
                tooltipView.requestLayout()
                binding.itemOverlay.visibility = View.VISIBLE
                arrowView.visibility = View.VISIBLE
                itemImageView.visibility = View.VISIBLE
                tooltipView.setTooltipText(
                    getString(R.string.tooltip_lesson_grammar)

                )
                slideInAnimation(tooltipView)
                PrefManager.put(HAS_SEEN_GRAMMAR_ANIMATION, true)
            }
        }catch (ex:Exception){
            ex.printStackTrace()
        }
    }

    private fun showIntroVideoUi() {
        viewModel.saveIntroVideoFlowImpression(INTRO_VIDEO_STARTED_PLAYING)
        videoEvent.postValue(
            Event(VideoModel(video_url = introVideoUrl, id = INTRO_VIDEO_ID))
        )
        binding.videoCallBtn.setOnClickListener {
            PrefManager.put(IS_CALL_BTN_CLICKED_FROM_NEW_SCREEN, true)
            viewModel.saveIntroVideoFlowImpression(CALL_BUTTON_CLICKED_FROM_NEW_SCREEN)
            callPracticePartner()
            MixPanelTracker.publishEvent(MixPanelEvent.CALL_PRACTICE_PARTNER)
                .addParam(ParamKeys.LESSON_ID, getLessonId)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                .addParam(ParamKeys.VIA, "intro video complete")
                .push()
        }
    }

    private fun closeVideoPopUpUi() {
        binding.videoPopup.visibility = View.GONE
        binding.spotlightCallBtn.visibility = View.GONE
        binding.videoCallBtn.visibility = View.INVISIBLE
        binding.videoCallBtnText.visibility = View.INVISIBLE
        viewModel.showHideSpeakingFragmentCallButtons(2)
        binding.arrowAnimationnVideo.visibility = View.INVISIBLE
        binding.overlayLayout.visibility = View.GONE
        binding.overlayLayoutSpeaking.visibility = View.GONE
        binding.videoView.onStop()
        if (videoEvent.value?.peekContent()?.id == INTRO_VIDEO_ID) {
            if (lastVideoWatchedDuration > d2pIntroVideoWatchedDuration) {
                lastVideoWatchedDuration = 0
            }
            viewModel.saveIntroVideoFlowImpression(
                TIME_SPENT_ON_INTRO_VIDEO,
                (d2pIntroVideoWatchedDuration - lastVideoWatchedDuration)
            )
            lastVideoWatchedDuration = d2pIntroVideoWatchedDuration
        }
    }

    private fun setUpVideoProgressListener() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { mediaProgressEvent ->
                        val videoPercent =
                            binding.videoView.player?.duration?.let {
                                mediaProgressEvent.progress.div(
                                    it
                                ).times(100).toInt()
                            } ?: -1
                        val percentVideoWatched =
                            mediaProgressEvent.watchTime.times(100).div(
                                binding.videoView.player?.duration!!
                            ).toInt()

                        if (percentVideoWatched != 0) {
                            d2pIntroVideoWatchedDuration = mediaProgressEvent.watchTime
                        }

                        if (videoPercent != 0 && videoPercent >= 80) {
                            binding.videoCallBtn.visibility = View.VISIBLE
                            binding.videoCallBtnText.visibility = View.VISIBLE
                            binding.arrowAnimationnVideo.visibility = View.VISIBLE
                            viewModel.isD2pIntroVideoComplete(true)
                        }
                        if (videoPercent != 0 && videoPercent >= 80 && !isIntroVideoCmpleted) {
                            viewModel.saveIntroVideoFlowImpression(SIV_VIDEO_COMPLETED)
                            MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_VIDEO_COMPLETE)
                                .addParam(ParamKeys.LESSON_ID, getLessonId)
                                .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                                .push()
                            isIntroVideoCmpleted = true
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun closeReadingFullScreen() {
        supportFragmentManager.popBackStackImmediate()
        container_reading.visibility = View.GONE
    }
}

@BindingAdapter("setRatingText")
fun AppCompatTextView.ratingText(rating: UserRating?) {
    Log.d(TAG, "ratingText: $rating")
    if (rating != null) {
        if (rating.rating.toInt() == -1 || rating.rating.isNaN()) {
            this.visibility = View.GONE
        } else {
            this.visibility = View.VISIBLE
            this.background.setColorFilter(Color.parseColor(rating.bgColor), PorterDuff.Mode.SRC_ATOP)
            this.setTextColor(Color.parseColor(rating.color))
            this.text = "Your Rating: ${rating.rating.toString()}"
        }
    }
}
