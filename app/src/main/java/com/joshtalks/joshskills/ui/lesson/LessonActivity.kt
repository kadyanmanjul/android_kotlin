package com.joshtalks.joshskills.ui.lesson

import android.Manifest.*
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Outline
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.PERMISSION_FROM_READING
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.extension.translationAnimationNew
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.core.videotranscoder.recyclerView
import com.joshtalks.joshskills.databinding.LessonActivityBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AnimateAtsOtionViewEvent
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GRAMMAR_ANIMATION
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.lesson.room.ConversationRoomListingPubNubFragment
import com.joshtalks.joshskills.ui.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.lesson.vocabulary.VocabularyFragment
import com.joshtalks.joshskills.ui.online_test.GrammarAnimation
import com.joshtalks.joshskills.ui.online_test.GrammarOnlineTestFragment
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val GRAMMAR_POSITION = 0
const val SPEAKING_POSITION = 1
const val VOCAB_POSITION = 2
const val READING_POSITION = 3
const val ROOM_POSITION = 4
const val DEFAULT_SPOTLIGHT_DELAY_IN_MS = 1300L
private const val TAG = "LessonActivity"
const val TOOLTIP_LESSON_GRAMMAR = "TOOLTIP_LESSON_GRAMMAR_"
val STORAGE_GRAMMER_REQUEST_CODE = 3456
private val STORAGE_READING_REQUEST_CODE = 3457

class LessonActivity : WebRtcMiddlewareActivity(), LessonActivityListener, GrammarAnimation {
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
    private var isNewGrammar = false
    private var isLesssonCompleted = false
    private var testId = -1
    private var whatsappUrl = EMPTY
    private val compositeDisposable = CompositeDisposable()
//    private var customView: CustomWord? = null
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
    private val adapter: LessonPagerAdapter by lazy {
        LessonPagerAdapter(
            supportFragmentManager,
            this.lifecycle,
            arrayFragment,
            viewModel.lessonIsConvoRoomActive
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

        event.observe(this) {
            when(it.what) {
                PERMISSION_FROM_READING -> requestStoragePermission(STORAGE_READING_REQUEST_CODE)
            }
        }

        PrefManager.put(LESSON_COMPLETE_SNACKBAR_TEXT_STRING, EMPTY, false)
        val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0
        if (lessonId == 0) {
            // InboxActivity.startInboxActivity(this)
            finish()
        }
        viewModel.isFreeTrail = if (intent.hasExtra(IS_FREE_TRAIL)) intent.getBooleanExtra(
            IS_FREE_TRAIL,
            false
        ) else false
        isDemo = if (intent.hasExtra(IS_DEMO)) intent.getBooleanExtra(IS_DEMO, false) else false
        isNewGrammar = if (intent.hasExtra(IS_NEW_GRAMMAR)) intent.getBooleanExtra(
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
        viewModel.getWhatsappRemarketingCampaign(CampaignKeys.WHATSAPP_REMARKETING.name)
        viewModel.getLesson(lessonId)
        viewModel.getTwentyMinFtuCallCampaignData(CampaignKeys.TWENTY_MIN_TARGET.NAME, lessonId, isDemo)

        val helpIv: ImageView = findViewById(R.id.iv_help)
        helpIv.visibility = View.GONE
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
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
             RxBus2.listenWithoutDelay(AnimateAtsOtionViewEvent::class.java)
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe { event ->
                    CustomWord(this, event.customWord.choice).apply {
                        binding.rootView.addView(this)
                         this.text = event.customWord.choice.text
                         this.x = event.fromLocation[0].toFloat()
                         this.y = event.fromLocation[1].toFloat() - event.height.toFloat()
                         val toLocation = IntArray(2)
                         event.customWord.getLocationOnScreen(toLocation)
                        toLocation[1] = toLocation[1] - getStatusBarHeight()
                        this.translationAnimationNew(
                             toLocation,
                             event.customWord,
                            event.optionLayout,
                            doOnAnimationEnd = {
                                binding.rootView.removeView(this)
                            }
                         )
                     }
                 }
        )
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setObservers() {

        viewModel.lessonQuestionsLiveData.observe(
            this
        ) {
            binding.progressView.visibility = View.GONE
            viewModel.lessonLiveData.value?.let {
                titleView.text =
                    getString(R.string.lesson_no, it.lessonNo)
                lessonNumber = it.lessonNo
                lessonIsNewGrammar = it.isNewGrammar
            }
            viewModel.lessonIsConvoRoomActive =
                (it.filter { it.chatType == CHAT_TYPE.CR }.isNotEmpty()
                        && PrefManager.getBoolValue(IS_CONVERSATION_ROOM_ACTIVE_FOR_USER))
            //viewModel.lessonIsConvoRoomActive  = true

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
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.VISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
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
                    if(introVideoControl){
                        if (introVideoUrl.isNullOrBlank().not()) {
                        viewModel.saveIntroVideoFlowImpression(SPEAKING_TAB_CLICKED_FOR_FIRST_TIME)
                        viewModel.showHideSpeakingFragmentCallButtons(1)
                        showIntroVideoUi()
                        }
                 //   }else if(PrefManager.getBoolValue(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL) || !isTwentyMinFtuCallActive){
                    }else if(PrefManager.getBoolValue(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL)){
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

                LessonSpotlightState.CONVO_ROOM_SPOTLIGHT -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.spotlightTabConvo.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.INVISIBLE
                    binding.convoRoomSpotlightTooltip.visibility = View.VISIBLE
                    binding.convoRoomSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
                else -> {
                    // Hide lesson Spotlight
                    binding.overlayLayout.visibility = View.GONE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.spotlightTabConvo.visibility = View.INVISIBLE
                    binding.convoRoomSpotlightTooltip.visibility = View.GONE
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
        viewModel.whatsappRemarketingLiveData.observe(this) { abTestCampaignData ->
            abTestCampaignData?.let { map ->
                isWhatsappRemarketingActive =
                    (map.variantKey == VariantKeys.WR_ENABLED.NAME) && map.variableMap?.isEnabled == true
            }
        }

        viewModel.twentyMinCallFtuAbTestLiveData.observe(this) { abTestCampaignData ->
            abTestCampaignData?.let { map ->
                isTwentyMinFtuCallActive =
                    (map.variantKey == VariantKeys.TWENTY_MIN_ENABLED.NAME) && map.variableMap?.isEnabled == true
                isTwentyMinFtuCallActive = true
                PrefManager.put(IS_TWENTY_MIN_CALL_ENABLED, isTwentyMinFtuCallActive)
            //    showToast(PrefManager.getBoolValue(IS_TWENTY_MIN_CALL_ENABLED).toString())
            }
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
        if(introVideoControl) closeIntroVideoPopUpUi()
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

                    if (viewModel.lessonIsConvoRoomActive) {
                        lessonCompleted = lessonCompleted &&
                                lesson.conversationStatus == LESSON_STATUS.CO
                    }

                    if (lessonCompleted) {
                        PrefManager.put(LESSON_COMPLETED_FOR_NOTIFICATION, true)
                        if (lesson.status != LESSON_STATUS.CO) {
                            MarketingAnalytics.logLessonCompletedEvent(lesson.lessonNo)
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

                    if (viewModel.lessonIsConvoRoomActive) {
                        lessonCompleted = lessonCompleted &&
                                lesson.conversationStatus == LESSON_STATUS.CO
                    }

                    if (lessonCompleted) {
                        if (lesson.status != LESSON_STATUS.CO) {
                            MarketingAnalytics.logLessonCompletedEvent(lesson.lessonNo)
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
            introVideoControl  = true
            setUpVideoProgressListener()
            viewModel.getVideoData()
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
                            if(isWhatsappRemarketingActive){
                                MarketingAnalytics.logWhatsappRemarketing()
                            }
                        }
                        lesson.grammarStatus = status
                    }
                    VOCAB_POSITION -> lesson.vocabStatus = status
                    READING_POSITION -> lesson.readingStatus = status
                    SPEAKING_POSITION -> {
                        if (lesson.speakingStatus != LESSON_STATUS.CO && status == LESSON_STATUS.CO) {
                            MarketingAnalytics.logSpeakingSectionCompleted()
                        }
                        lesson.speakingStatus = status
                    }
                    ROOM_POSITION -> lesson.conversationStatus = status
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

        if (lessonIsNewGrammar) {
            if (isTestCompleted.not()) {
                arrayFragment.add(GRAMMAR_POSITION, GrammarOnlineTestFragment.getInstance(lessonNo))
            } else if (PrefManager.getIntValue(
                    ONLINE_TEST_LAST_LESSON_COMPLETED
                ) >= lessonNumber
            ) {
                arrayFragment.add(GRAMMAR_POSITION, GrammarOnlineTestFragment.getInstance(lessonNo))

            } else arrayFragment.add(GRAMMAR_POSITION, GrammarFragment.getInstance())
        } else {
            arrayFragment.add(GRAMMAR_POSITION, GrammarFragment.getInstance())
        }

        arrayFragment.add(SPEAKING_POSITION, SpeakingPractiseFragment.newInstance())
        arrayFragment.add(VOCAB_POSITION, VocabularyFragment.getInstance())
        arrayFragment.add(READING_POSITION, ReadingFragmentWithoutFeedback.getInstance())
        if (viewModel.lessonIsConvoRoomActive) {
            arrayFragment.add(ROOM_POSITION, ConversationRoomListingPubNubFragment.getInstance())
        }
        binding.lessonViewpager.adapter = adapter
        binding.lessonViewpager.requestTransparentRegion(binding.lessonViewpager)
        binding.lessonViewpager.offscreenPageLimit = 4
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
                VOCAB_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.VOCABULARY_TITLE)
                }
                READING_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.READING_TITLE)
                }
                SPEAKING_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv).text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.SPEAKING_TITLE)
                }
                ROOM_POSITION -> {
                    setUnselectedColor(tab)
                    tab.view.findViewById<TextView>(R.id.title_tv)?.text =
                        AppObjectController.getFirebaseRemoteConfig()
                            .getString(FirebaseRemoteConfigKey.ROOM_TITLE)
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
                    binding.lessonViewpager.currentItem = defaultSection
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
                        VOCAB_POSITION ->
                            if (lesson.vocabStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = VOCAB_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        READING_POSITION ->
                            if (lesson.readingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = READING_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        SPEAKING_POSITION ->
                            if (lesson.speakingStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = SPEAKING_POSITION
                                return
                            } else {
                                nextTabIndex++
                            }
                        ROOM_POSITION ->
                            if (lesson.conversationStatus != LESSON_STATUS.CO) {
                                binding.lessonViewpager.currentItem = ROOM_POSITION
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
                    tabs.getChildAt(VOCAB_POSITION),
                    lesson.vocabStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(READING_POSITION),
                    lesson.readingStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(SPEAKING_POSITION),
                    lesson.speakingStatus == LESSON_STATUS.CO
                )
                setTabCompletionStatus(
                    tabs.getChildAt(ROOM_POSITION),
                    lesson.conversationStatus == LESSON_STATUS.CO
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
                }
                VOCAB_POSITION -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.vocabulary_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_VOCABULARY_SCREEN)
                }
                READING_POSITION -> {
                    tab.view.background = ContextCompat.getDrawable(this, R.drawable.reading_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_READING_SCREEN)
                }
                ROOM_POSITION -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.convo_room_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_ROOM_SCREEN)
                    if (PrefManager.getBoolValue(HAS_SEEN_CONVO_ROOM_SPOTLIGHT)) {
                        hideSpotlight()
                    } else {
                        showConvoRoomSpotlight()
                    }
                }
                SPEAKING_POSITION -> {
                    tab.view.background =
                        ContextCompat.getDrawable(this, R.drawable.speaking_tab_bg)
                    viewModel.saveImpression(IMPRESSION_OPEN_SPEAKING_SCREEN)
                    if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT)) {
                        hideSpotlight()
                    } else {
                        showSpeakingSpotlight()
                    }
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

    private fun showConvoRoomSpotlight() {
        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.CONVO_ROOM_SPOTLIGHT)
        PrefManager.put(HAS_SEEN_CONVO_ROOM_SPOTLIGHT, true)
        binding.overlayLayout.setOnClickListener {
            hideSpotlight()
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
        this.lesson = lesson
        openLessonCompletedActivity.launch(
            LessonCompletedActivity.getActivityIntent(
                this,
                lesson
            )
        )
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
        intent?.let {
            val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0

            viewModel.getLesson(lessonId)
            viewModel.getTwentyMinFtuCallCampaignData(CampaignKeys.TWENTY_MIN_TARGET.NAME, lessonId, isDemo)
        }
    }

    override fun onPause() {
        if(introVideoControl) binding.videoView.onPause()
        super.onPause()
    }

    override fun onBackPressed() {
        when {
            binding.itemOverlay.isVisible -> binding.itemOverlay.isVisible = false
            binding.overlayTooltipLayout.isVisible -> showVideoToolTip(false)
            binding.videoPopup.isVisible -> closeIntroVideoPopUpUi()
            isVideoVisible.value == true -> isVideoVisible.value = false
            binding.overlayLayout.isVisible -> hideSpotlight()
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
        const val IS_FREE_TRAIL = "is_free_trail"
        const val IS_NEW_GRAMMAR = "is_new_grammar"
        const val IS_LESSON_COMPLETED = "is_lesson_completed"
        private const val WHATSAPP_URL = "whatsapp_url"
        private const val TEST_ID = "test_id"
        const val LAST_LESSON_STATUS = "last_lesson_status"
        const val LESSON_SECTION = "lesson_section"
        val isVideoVisible = MutableLiveData(false)

        fun getActivityIntent(
            context: Context,
            lessonId: Int,
            isDemo: Boolean = false,
            whatsappUrl: String? = null,
            testId: Int? = null,
            conversationId: String? = null,
            isNewGrammar: Boolean = false,
            isFreeTrail: Boolean = false,
            isLessonCompleted: Boolean = false
        ) = Intent(context, LessonActivity::class.java).apply {
            // TODO: Pass Free Trail Status
            putExtra(LESSON_ID, lessonId)
            putExtra(IS_DEMO, isDemo)
            putExtra(IS_NEW_GRAMMAR, isNewGrammar)
            putExtra(IS_LESSON_COMPLETED, isLessonCompleted)
            putExtra(CONVERSATION_ID, conversationId)
            putExtra(IS_FREE_TRAIL, isFreeTrail)
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
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(TOOLTIP_LESSON_GRAMMAR + courseId)

            )
            slideInAnimation(tooltipView)
            PrefManager.put(HAS_SEEN_GRAMMAR_ANIMATION, true)
        }
    }

    private fun showIntroVideoUi() {
        binding.overlayLayout.visibility = View.GONE
        binding.overlayLayoutSpeaking.visibility = View.VISIBLE
        viewModel.showHideSpeakingFragmentCallButtons(1)
        binding.videoPopup.visibility = View.VISIBLE
        binding.videoView.seekToStart()
        binding.spotlightTabGrammar.visibility = View.INVISIBLE
        binding.spotlightTabSpeaking.visibility = View.INVISIBLE
        binding.spotlightTabVocab.visibility = View.INVISIBLE
        binding.spotlightTabReading.visibility = View.INVISIBLE
        binding.spotlightCallBtn.visibility = View.GONE
        binding.spotlightCallBtnText.visibility = View.GONE

        binding.videoView.setUrl(introVideoUrl)
        binding.videoView.onStart()
        viewModel.saveIntroVideoFlowImpression(INTRO_VIDEO_STARTED_PLAYING)
        binding.videoView.setPlayListener {
            val currentVideoProgressPosition = binding.videoView.progress
            openVideoPlayerActivity.launch(
                VideoPlayerActivity.getActivityIntent(
                    this,
                    "",
                    null,
                    introVideoUrl,
                    currentVideoProgressPosition,
                    conversationId = getConversationId()
                )
            )
        }

        lifecycleScope.launchWhenStarted {
            binding.videoView.downloadStreamPlay()
        }

        binding.imageViewClose.setOnClickListener {
            closeIntroVideoPopUpUi()
        }

        binding.videoView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 15f)
            }
        }
        binding.videoView.clipToOutline = true

        binding.videoCallBtn.setOnClickListener {
            PrefManager.put(IS_CALL_BTN_CLICKED_FROM_NEW_SCREEN, true)
            viewModel.saveIntroVideoFlowImpression(CALL_BUTTON_CLICKED_FROM_NEW_SCREEN)
            callPracticePartner()
        }
    }

    private fun closeIntroVideoPopUpUi() {
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
        if (lastVideoWatchedDuration > d2pIntroVideoWatchedDuration) {
            lastVideoWatchedDuration = 0
        }
        viewModel.saveIntroVideoFlowImpression(
            TIME_SPENT_ON_INTRO_VIDEO,
            (d2pIntroVideoWatchedDuration - lastVideoWatchedDuration)
        )
        lastVideoWatchedDuration = d2pIntroVideoWatchedDuration
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
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }


}
