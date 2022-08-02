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
import android.widget.VideoView
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
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.CLOSE_FULL_READING_FRAGMENT
import com.joshtalks.joshskills.constants.OPEN_READING_SHARING_FULLSCREEN
import com.joshtalks.joshskills.constants.PERMISSION_FROM_READING
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.ApiCallStatus.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
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
import com.joshtalks.joshskills.repository.server.course_detail.VideoModel
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GRAMMAR_ANIMATION
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
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
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.voip.constant.State
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.lesson_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val GRAMMAR_POSITION = 0
const val TRANSLATION_POSITION = 1
const val SPEAKING_POSITION = 2
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

        try {
            if (getVoipState() == State.IDLE)
                viewModel.getButtonVisibility()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.lesson_activity
        )
        binding.viewbinding = this
        event.observe(this) {
            when (it.what) {
                PERMISSION_FROM_READING -> requestStoragePermission(STORAGE_READING_REQUEST_CODE)
                OPEN_READING_SHARING_FULLSCREEN -> openReadingFullScreen()
                CLOSE_FULL_READING_FRAGMENT -> closeReadingFullScreen()
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
        lessonIsNewGrammar = if (intent.hasExtra(IS_NEW_GRAMMAR)) intent.getBooleanExtra(
            IS_NEW_GRAMMAR,
            false
        ) else false

        if (intent.hasExtra(IS_LESSON_COMPLETED)) {
            isLesssonCompleted = intent.getBooleanExtra(IS_LESSON_COMPLETED, false)
        } else {
            isLesssonCompleted = false
        }

        if (intent.hasExtra(LESSON_SECTION)) {
            defaultSection = intent.getIntExtra(LESSON_SECTION, 0)
        }

        whatsappUrl =
            if (intent.hasExtra(WHATSAPP_URL) && intent.getStringExtra(WHATSAPP_URL).isNullOrBlank()
                    .not()
            ) intent.getStringExtra(WHATSAPP_URL) ?: EMPTY else EMPTY
        testId = intent.getIntExtra(TEST_ID, -1)

        titleView = findViewById(R.id.text_message_title)

        setObservers()
        viewModel.getLesson(lessonId)
        viewModel.getQuestions(lessonId, isDemo)

        val helpIv: ImageView = findViewById(R.id.iv_help)
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            onBackPressed()
        }
        if (isDemo) {
            binding.buyCourseLl.visibility = View.VISIBLE
        }
        if (PrefManager.getBoolValue(HAS_SEEN_QUIZ_VIDEO_TOOLTIP).not()) {
            binding.tooltipFrame.setOnClickListener { showVideoToolTip(false) }
            binding.overlayTooltipLayout.setOnClickListener { showVideoToolTip(false) }
            binding.tooltipTv.setOnClickListener { showVideoToolTip(false) }
        }
        viewModel.saveImpression(IMPRESSION_OPEN_GRAMMAR_SCREEN)
        binding.imageViewClose.setOnClickListener {
            closeVideoPopUpUi()
        }
        viewModel.lessonId.postValue(getLessonId)
    }

    private fun requestStoragePermission(requestCode: Int) {
        PermissionUtils.storageReadAndWritePermissionReading(
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
                titleView.text =
                    getString(R.string.lesson_no, it.lessonNo)
                lessonNumber = it.lessonNo
                lessonIsNewGrammar = it.isNewGrammar
            }
            MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_OPENED)
                .addParam(ParamKeys.LESSON_ID, getLessonId)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                .push()
            viewModel.postGoal(GoalKeys.GRAMMAR_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
            if(lessonNumber == 1) {
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.GRAMMAR_SPOTLIGHT_PART1)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART1)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART2)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART3)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.READING_SPOTLIGHT)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                    lifecycleScope.launch {
                        delay(DEFAULT_SPOTLIGHT_DELAY_IN_MS)
                        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.GRAMMAR_SPOTLIGHT_PART2)
                    }
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.VISIBLE
                }

                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    if (introVideoControl) {
                        if (introVideoUrl.isNullOrBlank().not()) {
                            viewModel.saveIntroVideoFlowImpression(
                                SPEAKING_TAB_CLICKED_FOR_FIRST_TIME
                            )
                            viewModel.showHideSpeakingFragmentCallButtons(1)
                            showIntroVideoUi()
                        }
                    } else if (PrefManager.getBoolValue(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL)) {
                        binding.overlayLayout.visibility = View.VISIBLE
                        binding.spotlightTabGrammar.visibility = View.INVISIBLE
                        binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                        binding.spotlightTabVocab.visibility = View.INVISIBLE
                        binding.spotlightTabReading.visibility = View.INVISIBLE
                        binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                        binding.lessonSpotlightTooltip.setTooltipText(
                            resources.getText(R.string.label_speaking_spotlight_2).toString()
                        )
                        binding.lessonSpotlightTooltip.post {
                            slideInAnimation(binding.lessonSpotlightTooltip)
                        }
                        binding.spotlightStartGrammarTest.visibility = View.GONE
                        binding.spotlightCallBtn.visibility = View.VISIBLE
                        binding.spotlightCallBtnText.visibility = View.VISIBLE
                        binding.arrowAnimation.visibility = View.VISIBLE
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
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
            }
        }

        viewModel.introVideoLiveDataForSpeakingSection.observe(this) {
            introVideoUrl = it.videoLink
            if (introVideoUrl.isNullOrBlank()) {
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
                    spotlightCallBtnText.visibility = View.GONE
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
                        setVideoId(it.id)
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
    }

    private fun hideSpotlight() {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
    }

    private fun showLessonSpotlight() {
        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.LESSON_SPOTLIGHT)
        PrefManager.put(HAS_SEEN_LESSON_SPOTLIGHT, true)
    }

    fun startOnlineExamTest() {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        viewModel.grammarSpotlightClickLiveData.postValue(Unit)
    }

    fun callPracticePartner() {
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        viewModel.speakingSpotlightClickLiveData.postValue(Unit)
        if (introVideoControl) closeVideoPopUpUi()
    }

    private fun openReadingFullScreen() {
        binding.containerReading.visibility = View.VISIBLE
        supportFragmentManager.commit {
            val fragment = ReadingFullScreenFragment()
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
                        }
                        lesson.status = LESSON_STATUS.CO
                        viewModel.updateLesson(lesson)
                        AppObjectController.uiHandler.post {
                            openLessonCompleteScreen(lesson)
                        }
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
                        if (lesson.status != LESSON_STATUS.CO) {
                            MarketingAnalytics.logLessonCompletedEvent(lesson.lessonNo, lesson.id)
                        }
                        lesson.status = LESSON_STATUS.CO
                        viewModel.updateLesson(lesson)
                        AppObjectController.uiHandler.post {
                            openLessonCompleteScreen(lesson)
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
                    GRAMMAR_POSITION -> {
                        if (lesson.grammarStatus != LESSON_STATUS.CO && status == LESSON_STATUS.CO) {
                            MarketingAnalytics.logGrammarSectionCompleted()
                            if (isWhatsappRemarketingActive) {
                                MarketingAnalytics.logWhatsappRemarketing()
                            }
                        }
                        lesson.grammarStatus = status
                    }
                    SPEAKING_POSITION -> {
                        if (lesson.speakingStatus != LESSON_STATUS.CO && status == LESSON_STATUS.CO) {
                            MarketingAnalytics.logSpeakingSectionCompleted()
                            MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_COMPLETED)
                                .addParam(ParamKeys.LESSON_ID, getLessonId)
                                .addParam(ParamKeys.LESSON_NUMBER, lesson.lessonNo)
                                .push()
                        }
                        lesson.speakingStatus = status
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

        isTranslationDisabled = 1
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

        arrayFragment.add(
            SPEAKING_POSITION - isTranslationDisabled,
            SpeakingPractiseFragment.newInstance()
        )
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
                GRAMMAR_POSITION -> {
                    setSelectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.GRAMMAR_TITLE)
                }
                SPEAKING_POSITION - isTranslationDisabled -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SPEAKING_TITLE)
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
                setSelectedColor(tab)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                setSelectedColor(tab)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                setUnselectedColor(tab)
            }
        })

        Handler().postDelayed(
            {
                if (defaultSection != -1) {
                    binding.lessonViewpager.currentItem =
                        if (defaultSection == GRAMMAR_POSITION) GRAMMAR_POSITION else defaultSection - isTranslationDisabled
                } else {
                    openIncompleteTab(arrayFragment.size - 1)
                }
            },
            50
        )
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
                        GRAMMAR_POSITION ->
                            if (lesson.grammarStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = GRAMMAR_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        SPEAKING_POSITION - isTranslationDisabled ->
                            if (lesson.speakingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem =
                                    SPEAKING_POSITION - isTranslationDisabled
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

    private fun setTabCompletionStatus() {
        try {
            viewModel.lessonLiveData.value?.let { lesson ->
                if (lesson.lessonNo >= 2) {
                    PrefManager.put(LESSON_TWO_OPENED, true)
                }
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
                setTabCompletionStatus(
                    tabs.getChildAt(SPEAKING_POSITION - isTranslationDisabled),
                    lesson.speakingStatus == LESSON_STATUS.CO
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

            tab.view.findViewById<TextView>(R.id.title_tv)
                ?.setTextColor(ContextCompat.getColor(this, R.color.white))

            when (tab.position) {
                GRAMMAR_POSITION -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.capsule_selection_tab)
                    viewModel.saveImpression(IMPRESSION_OPEN_GRAMMAR_SCREEN)
                    MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                VOCAB_POSITION - isTranslationDisabled -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.vocabulary_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_VOCABULARY_SCREEN)
                    viewModel.postGoal(GoalKeys.VOCABULARY_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.VOCABULARY_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                READING_POSITION - isTranslationDisabled -> {
                    tab.view.background = ContextCompat.getDrawable(this, R.drawable.reading_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_READING_SCREEN)
                    viewModel.postGoal(GoalKeys.READING_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.READING_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                }
                SPEAKING_POSITION - isTranslationDisabled -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.speaking_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_SPEAKING_SCREEN)
                    PrefManager.put(IS_SPEAKING_SCREEN_CLICKED, true)
                    viewModel.postGoal(GoalKeys.SPEAKING_SECTION_OPENED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_OPENED)
                        .addParam(ParamKeys.LESSON_ID, getLessonId)
                        .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
                        .push()
                    if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT)) {
                        hideSpotlight()
                    } else {
                        showSpeakingSpotlight()
                    }
                }
                TRANSLATION_POSITION -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.convo_room_tab_bg)
                }
            }
        }
    }

    private fun showSpeakingSpotlight() {
        if (lessonNumber == 1) {
            viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2)
            PrefManager.put(HAS_SEEN_SPEAKING_SPOTLIGHT, true)
        }
    }

    private fun setUnselectedColor(tab: TabLayout.Tab?) {
        tab?.let {
            tab.view.background = ContextCompat.getDrawable(this, R.drawable.unselected_tab_bg)
            tab.view.findViewById<TextView>(R.id.title_tv)
                ?.setTextColor(ContextCompat.getColor(this, R.color.black))
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

    fun buyCourse() {
        if (testId != -1) {
            PaymentSummaryActivity.startPaymentSummaryActivity(this, testId.toString())
        }
    }

    fun openWhatsapp() {
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
        if (intent?.getBooleanExtra("reopen",false)==true) {
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
        when {
            binding.itemOverlay.isVisible -> binding.itemOverlay.isVisible = false
            binding.overlayTooltipLayout.isVisible -> showVideoToolTip(false)
            binding.videoPopup.isVisible -> closeVideoPopUpUi()
            binding.overlayLayout.isVisible -> hideSpotlight()
            binding.containerReading.isVisible -> {
                Log.e("Ayaaz", "OnBackPressedddddd")
//                videoView.stopPlayback()
//                supportFragmentManager.remove(yourfragment).commit()

                supportFragmentManager.beginTransaction().remove(ReadingFullScreenFragment()).commit()
//                merged_video.stopPlayback()
                viewModel.closeVideoView()
                closeReadingFullScreen()
                viewModel.showVideoView()
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
        const val IS_NEW_GRAMMAR = "is_new_grammar"
        const val IS_LESSON_COMPLETED = "is_lesson_completed"
        private const val WHATSAPP_URL = "whatsapp_url"
        private const val TEST_ID = "test_id"
        const val LAST_LESSON_STATUS = "last_lesson_status"
        const val LESSON_SECTION = "lesson_section"
        val videoEvent: MutableLiveData<Event<VideoModel>> = MutableLiveData()

        fun getActivityIntent(
            context: Context,
            lessonId: Int,
            isDemo: Boolean = false,
            whatsappUrl: String? = null,
            testId: Int? = null,
            conversationId: String? = null,
            isNewGrammar: Boolean = false,
            isLessonCompleted: Boolean = false
        ) = Intent(context, LessonActivity::class.java).apply {
            // TODO: Pass Free Trail Status
            putExtra(LESSON_ID, lessonId)
            putExtra(IS_DEMO, isDemo)
            putExtra(IS_NEW_GRAMMAR, isNewGrammar)
            putExtra(IS_LESSON_COMPLETED, isLessonCompleted)
            putExtra(CONVERSATION_ID, conversationId)
            if (isDemo) {
                putExtra(WHATSAPP_URL, whatsappUrl)
                putExtra(TEST_ID, testId)
            }
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
        binding.spotlightCallBtnText.visibility = View.GONE
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
    private fun closeReadingFullScreen(){
        supportFragmentManager.popBackStackImmediate()
        container_reading.visibility = View.GONE
    }
}

@BindingAdapter("setRatingText")
fun AppCompatTextView.ratingText(rating:UserRating?){
    Log.d(TAG, "ratingText: $rating")
    if(rating!=null) {
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
