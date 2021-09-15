package com.joshtalks.joshskills.ui.lesson

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.extension.transaltionAnimationNew
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.core.videotranscoder.recyclerView
import com.joshtalks.joshskills.databinding.LessonActivityBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AnimateAtsOtionViewEvent
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_ID
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.lesson.grammar.GrammarFragment
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.joshtalks.joshskills.ui.lesson.lesson_completed.LessonCompletedActivity
import com.joshtalks.joshskills.ui.lesson.reading.ReadingFragmentWithoutFeedback
import com.joshtalks.joshskills.ui.lesson.speaking.SpeakingPractiseFragment
import com.joshtalks.joshskills.ui.lesson.vocabulary.VocabularyFragment
import com.joshtalks.joshskills.ui.online_test.GrammarOnlineTestFragment
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_LESSON_INTERVAL
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

const val GRAMMAR_POSITION = 0
const val SPEAKING_POSITION = 1
const val VOCAB_POSITION = 2
const val READING_POSITION = 3
const val DEFAULT_SPOTLIGHT_DELAY_IN_MS = 1300L

class LessonActivity : WebRtcMiddlewareActivity(), LessonActivityListener {

    private lateinit var binding: LessonActivityBinding

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }

    lateinit var titleView: TextView
    private var isDemo = false
    private var isNewGrammar = false
    private var isLesssonCompleted = false
    private var testId = -1
    private var whatsappUrl = EMPTY
    private val compositeDisposable = CompositeDisposable()
    private var customView: CustomWord? = null
    var lesson: LessonModel? = null // Do not use this var
    private lateinit var tabs: ViewGroup
    val arrayFragment = arrayListOf<Fragment>()
    var lessonIsNewGrammar = false
    var lessonNumber = -1
    var defaultSection = -1
    var hasNotification = false
    private var ruleIdLeftList = ArrayList<Int>()
    private var ruleCompletedList: ArrayList<Int>? = arrayListOf()
    private var totalRuleList: ArrayList<Int>? = arrayListOf()
    private val adapter: LessonPagerAdapter by lazy {
        LessonPagerAdapter(
            supportFragmentManager,
            this.lifecycle,
            arrayFragment
        )
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
        PrefManager.put(LESSON_COMPLETE_SNACKBAR_TEXT_STRING, EMPTY, false)
        val lessonId = if (intent.hasExtra(LESSON_ID)) intent.getIntExtra(LESSON_ID, 0) else 0
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

        if (intent.hasExtra(HAS_NOTIFICATION)) {
            hasNotification = intent.getBooleanExtra(HAS_NOTIFICATION, false)
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
            onBackPressed()
        }
        if (isDemo) {
            binding.buyCourseLl.visibility = View.VISIBLE
        }
        viewModel.saveImpression(IMPRESSION_OPEN_GRAMMAR_SCREEN)
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
                    if (customView == null) {
                        customView = CustomWord(this, event.customWord.choice)
                    } else {
                        binding.rootView.removeView(customView)
                        customView?.updateChoice(event.customWord.choice)
                        //customView?.choice = event.customWord.choice
                    }
                    customView?.apply {
                        binding.rootView.addView(this)
                        this.text = event.customWord.choice.text
                        this.x = event.fromLocation[0].toFloat()
                        this.y = event.fromLocation[1].toFloat() - event.height.toFloat()
                        val toLocation = IntArray(2)
                        event.customWord.getLocationOnScreen(toLocation)
                        toLocation[1] = toLocation[1] - (event.height) + CustomWord.mPaddingTop
                        this.transaltionAnimationNew(
                            toLocation,
                            event.customWord,
                            event.optionLayout
                        )
                    }
                }
        )
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun setObservers() {

//        viewModel.lessonLiveData.observe(this, {
//            setUpTabLayout()
//            setTabCompletionStatus()
//        })

        viewModel.lessonQuestionsLiveData.observe(
            this,
            {
                binding.progressView.visibility = View.GONE
                viewModel.lessonLiveData.value?.let {
                    titleView.text =
                        getString(R.string.lesson_no, it.lessonNo)
                    lessonNumber = it.lessonNo
                    lessonIsNewGrammar = it.isNewGrammar
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
//                if (PrefManager.getBoolValue(HAS_SEEN_LESSON_SPOTLIGHT)) {
//                    hideSpotlight()
//                } else {
//                    showLessonSpotlight()
//                }
            }
        )

        viewModel.ruleListIds.observe(
            this,
            { ruleIds ->
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
        )

        viewModel.updatedLessonResponseLiveData.observe(
            this,
            {
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
                /*if (it.awardMentorList.isNullOrEmpty().not()) {
                    //TODO add when awards functionality is over
                    //ShowAwardFragment.showDialog(supportFragmentManager,it.awardMentorList!!)
                }
                if (it.outranked!!) {
                    it.outrankedData?.let {
                        showLeaderboardAchievement(
                            it,
                            lessonInterval,
                            chatId,
                            lessonModel?.lessonNo ?: 0
                        )
                    }
                }*/
            }
        )

        viewModel.pointsSnackBarText.observe(
            this,
            {
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, it.pointsList!!.get(0))
                    PrefManager.put(
                        LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                        it.pointsList!!.last(),
                        false
                    )
                }
            }
        )

        viewModel.lessonSpotlightStateLiveData.observe(this, {
            // Show lesson Spotlight
            when (it) {
                LessonSpotlightState.LESSON_SPOTLIGHT -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_lesson_spotlight)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_grammar_spotlight)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_speaking_spotlight)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_vocab_spotlight_1)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_vocab_spotlight_2)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_vocab_spotlight_3)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_reading_spotlight)
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
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_grammar_spotlight)
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.VISIBLE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.VISIBLE
                }
                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    binding.overlayLayout.visibility = View.VISIBLE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.VISIBLE
                    binding.lessonSpotlightTooltip.text =
                        resources.getText(R.string.label_speaking_spotlight_2)
                    binding.lessonSpotlightTooltip.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
                    )
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.VISIBLE
                    binding.spotlightCallBtnText.visibility = View.VISIBLE
                    binding.arrowAnimation.visibility = View.VISIBLE
                }
                else -> {
                    // Hide lesson Spotlight
                    binding.overlayLayout.visibility = View.GONE
                    binding.spotlightTabGrammar.visibility = View.INVISIBLE
                    binding.spotlightTabSpeaking.visibility = View.INVISIBLE
                    binding.spotlightTabVocab.visibility = View.INVISIBLE
                    binding.spotlightTabReading.visibility = View.INVISIBLE
                    binding.lessonSpotlightTooltip.visibility = View.GONE
                    binding.spotlightStartGrammarTest.visibility = View.GONE
                    binding.spotlightCallBtn.visibility = View.GONE
                    binding.spotlightCallBtnText.visibility = View.GONE
                    binding.arrowAnimation.visibility = View.GONE
                }
            }
        })
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
    }

//    fun onSpotlightClick() {
//        when (viewModel.lessonSpotlightStateLiveData.value) {
//            LessonSpotlightState.LESSON_SPOTLIGHT -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.GRAMMAR_SPOTLIGHT)
//            }
//            LessonSpotlightState.GRAMMAR_SPOTLIGHT -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT)
//            }
//            LessonSpotlightState.SPEAKING_SPOTLIGHT -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART1)
//            }
//            LessonSpotlightState.VOCAB_SPOTLIGHT_PART1 -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART2)
//            }
//            LessonSpotlightState.VOCAB_SPOTLIGHT_PART2 -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.VOCAB_SPOTLIGHT_PART3)
//            }
//            LessonSpotlightState.VOCAB_SPOTLIGHT_PART3 -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.READING_SPOTLIGHT)
//            }
//            LessonSpotlightState.READING_SPOTLIGHT -> {
//                viewModel.lessonSpotlightStateLiveData.postValue(null)
//            }
//        }
//    }

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
                    val lessonCompleted = lesson.grammarStatus == LESSON_STATUS.CO &&
                            lesson.vocabStatus == LESSON_STATUS.CO &&
                            lesson.readingStatus == LESSON_STATUS.CO &&
                            lesson.speakingStatus == LESSON_STATUS.CO

                    if (lessonCompleted) {
                        PrefManager.put(LESSON_COMPLETED_FOR_NOTIFICATION, true)
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
                    val lessonCompleted = lesson.grammarStatus == LESSON_STATUS.CO &&
                            lesson.vocabStatus == LESSON_STATUS.CO &&
                            lesson.readingStatus == LESSON_STATUS.CO &&
                            lesson.speakingStatus == LESSON_STATUS.CO

                    if (lessonCompleted) {
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

    override fun onSectionStatusUpdate(tabPosition: Int, isSectionCompleted: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.lessonLiveData.value?.let { lesson ->
                val status = if (isSectionCompleted) LESSON_STATUS.CO else LESSON_STATUS.NO
                when (tabPosition) {
                    GRAMMAR_POSITION -> lesson.grammarStatus = status
                    VOCAB_POSITION -> lesson.vocabStatus = status
                    READING_POSITION -> lesson.readingStatus = status
                    SPEAKING_POSITION -> lesson.speakingStatus = status
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
        } else if (ruleIdLeftList.isNullOrEmpty()) {
            return true
        } else return false
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
        viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2)
        PrefManager.put(HAS_SEEN_SPEAKING_SPOTLIGHT, true)
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
            Timber.d("ghjk12 : onNewIntentLessonId -> $lessonId")

            viewModel.getLesson(lessonId)
            viewModel.getQuestions(lessonId, isDemo)
        }
    }

    override fun onBackPressed() {
        if (binding.overlayLayout.visibility == View.VISIBLE) {
            hideSpotlight()
        } else {
            val resultIntent = Intent()
            viewModel.lessonLiveData.value?.let {
                resultIntent.putExtra(CHAT_ROOM_ID, it.chatId)
                resultIntent.putExtra(LAST_LESSON_INTERVAL, it.interval)
                resultIntent.putExtra(LAST_LESSON_STATUS, it.status?.name)
                resultIntent.putExtra(LESSON_NUMBER, it.lessonNo)
            }
            setResult(RESULT_OK, resultIntent)
            if (hasNotification) {
                InboxActivity.startInboxActivity(this)
            }
            this@LessonActivity.finish()
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
}
