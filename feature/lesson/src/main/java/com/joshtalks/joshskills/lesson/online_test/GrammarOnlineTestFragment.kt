package com.joshtalks.joshskills.lesson.online_test

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.common.BuildConfig
import com.joshtalks.joshskills.common.constants.GRAMMAR_POSITION
import com.joshtalks.joshskills.common.constants.TRANSLATION_POSITION
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.GRAMMAR_CONTINUE_BUTTON_TEXT
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.GRAMMAR_START_BUTTON_TEXT
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.GRAMMAR_TEST_COMPLETE_DESCRIPTION
import com.joshtalks.joshskills.common.core.abTest.CampaignKeys
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.repository.server.PurchasePopupType
import com.joshtalks.joshskills.common.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.lesson.online_test.util.A2C1Impressions
import com.joshtalks.joshskills.lesson.online_test.util.TestCompletedListener
import com.joshtalks.joshskills.common.ui.tooltip.TooltipUtils
import com.joshtalks.joshskills.lesson.LessonActivity
import com.joshtalks.joshskills.lesson.R
import com.joshtalks.joshskills.lesson.databinding.FragmentGrammarOnlineTestBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.*

class GrammarOnlineTestFragment : CoreJoshFragment(), TestCompletedListener {
    private lateinit var binding: FragmentGrammarOnlineTestBinding
    private var lessonActivityListener: com.joshtalks.joshskills.lesson.LessonActivityListener? = null

    private val viewModel: com.joshtalks.joshskills.lesson.LessonViewModel by lazy {
        ViewModelProvider(requireActivity())[com.joshtalks.joshskills.lesson.LessonViewModel::class.java]
    }
    private val viewModelOnlineTest: OnlineTestViewModel by lazy {
        ViewModelProvider(requireActivity())[OnlineTestViewModel::class.java]
    }
    private var lessonNumber: Int = -1
    private var lessonId: Int = -1
    private var scoreText: Int = -1
    private var pointsList: String? = null
    private var hasCompletedTest: Boolean = false

    private var timerPopText = EMPTY
    private var currentTooltipIndex = 0

    private var grammarAnimationListener: GrammarAnimation? = null


    private val lessonTooltipList by lazy {
        listOf(
            "हर पाठ में 4 भाग होते हैं \nGrammar, Vocabulary, Reading\nऔर Speaking",
//            "आज, इस भाग में हम अपने वर्तमान व्याकरण स्तर का पता लगाएंगे",
//            "हमारे स्तर के आधार पर अगले पाठ से हम यहाँ व्याकरण की अवधारणाएँ सीखेंगे"
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener3 = View.OnTouchListener { v, event ->
        val currentPaddingTop = v.paddingTop
        val currentPaddingBottom = v.paddingBottom
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isAdded && activity != null) {
                    val drawable = R.drawable.blue_new_btn_pressed_state
                    v.background = ContextCompat.getDrawable(
                        requireActivity(),
                        drawable
                    )
                    v.setPaddingRelative(
                        v.paddingLeft,
                        currentPaddingTop + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                        v.paddingRight,
                        currentPaddingBottom - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                    )
                    v.invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                if (isAdded && activity != null) {
                    val drawable = R.drawable.blue_new_btn_unpressed_state
                    v.background = ContextCompat.getDrawable(
                        requireActivity(),
                        drawable
                    )
                    v.setPaddingRelative(
                        v.paddingLeft,
                        currentPaddingTop - Utils.sdpToPx(R.dimen._1sdp).toInt(),
                        v.paddingRight,
                        currentPaddingBottom + Utils.sdpToPx(R.dimen._1sdp).toInt(),
                    )
                    v.invalidate()
                }
            }
        }
        false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is com.joshtalks.joshskills.lesson.LessonActivityListener)
            lessonActivityListener = context
        if (context is GrammarAnimation)
            grammarAnimationListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            lessonNumber = it.getInt(CURRENT_LESSON_NUMBER, -1)
            scoreText = it.getInt(SCORE_TEXT, -1)
            pointsList = it.getString(POINTS_LIST)
            hasCompletedTest = it.getBoolean(HAS_COMPLETED_TEST)
        }
        lessonId = if (requireActivity().intent.hasExtra(LessonActivity.LESSON_ID)) {
            requireActivity().intent.getIntExtra(LessonActivity.LESSON_ID, 0)
        } else 0
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_grammar_online_test,
                container,
                false
            )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = this
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueBtn.setOnTouchListener(onTouchListener3)
        binding.startBtn.setOnTouchListener(onTouchListener3)
        binding.scoreStartBtn.setOnTouchListener(onTouchListener3)
        // showTooltip()
        initBottomMargin()
        if (hasCompletedTest && PrefManager.getBoolValue(IS_FREE_TRIAL)) {
            viewModel.getCoursePopupData(PurchasePopupType.GRAMMAR_COMPLETED)
        }
        when {
            (PrefManager.getIntValue(ONLINE_TEST_LAST_LESSON_COMPLETED)
                .plus(1) == lessonNumber) -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                binding.testScoreContainer.visibility = View.GONE
                if (PrefManager.getIntValue(
                        ONLINE_TEST_LAST_LESSON_ATTEMPTED
                    ) == lessonNumber
                ) {
                    binding.description.text = getString(R.string.grammar_continue_test_text)
                    binding.startBtn.text = getContinueButtonText()
                } else {
                    binding.startBtn.text = getStartButtonText()
                }
            }
            (PrefManager.getIntValue(
                ONLINE_TEST_LAST_LESSON_COMPLETED
            ) >= lessonNumber) -> {
                binding.startTestContainer.visibility = View.GONE
                if (PrefManager.hasKey(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                        IS_FREE_TRIAL,
                        isConsistent = false,
                        defValue = false
                    ) || PrefManager.getStringValue(CURRENT_COURSE_ID) != DEFAULT_COURSE_ID
                ) {
                    binding.testScoreContainer.visibility = View.VISIBLE
                    binding.testCompletedContainer.visibility = View.GONE
                    if (scoreText != -1) {
                        binding.score.text = getString(R.string.test_score, scoreText)

                    } else {
                        binding.score.text = getString(
                            R.string.test_score, PrefManager.getIntValue(
                                FREE_TRIAL_TEST_SCORE, false, 0
                            )
                        )
                    }
                    if (pointsList.isNullOrBlank().not()) {
                        showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsList)
                        playSnackbarSound(requireContext())
                    }
                } else {
                    binding.testCompletedContainer.visibility = View.VISIBLE
                    binding.testScoreContainer.visibility = View.GONE
                    binding.confetti.playAnimation()
                    binding.lottieAnimationView.playAnimation()
                }
                completeGrammarCardLogic()
            }
            else -> {
                binding.startTestContainer.visibility = View.VISIBLE
                binding.testCompletedContainer.visibility = View.GONE
                binding.testScoreContainer.visibility = View.GONE
                binding.lockTestCard.visibility = View.VISIBLE
                if (BuildConfig.DEBUG &&  AppObjectController.applicationDetails.versionCode() >= 50006) {
                    binding.startBtn.text = getStartButtonText()
                    binding.startBtn.isEnabled = true
                    binding.startBtn.isClickable = true
                    binding.lockTestMessage.text =
                        "This will work in debug version. You will have to complete lesson ${
                            PrefManager.getIntValue(
                                ONLINE_TEST_LAST_LESSON_COMPLETED
                            ).plus(1)
                        } in the prod version before you can attempt this lesson"
                } else {
                    binding.startBtn.text = getStartButtonText()
                    binding.startBtn.isEnabled = false
                    binding.startBtn.isClickable = false
                    binding.startBtn.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.disabled
                        )
                    )
                    binding.lockTestMessage.text = getString(
                        R.string.grammar_lock_text, PrefManager.getIntValue(
                            ONLINE_TEST_LAST_LESSON_COMPLETED
                        ).plus(1)
                    )
                }
            }
        }

        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
        viewModel.grammarSpotlightClickLiveData.observe(viewLifecycleOwner) {
            startOnlineExamTest()
        }

        viewModel.eventLiveData.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                binding.startBtn.performClick()
            }
        }

        viewModel.lessonId.observe(
            viewLifecycleOwner
        ) {
            lessonId = it
        }

        viewModelOnlineTest.coursePopupData.observe(viewLifecycleOwner) {
            it?.let {
                com.joshtalks.joshskills.lesson.PurchaseDialog.newInstance(it)
                    .show(requireActivity().supportFragmentManager, "PurchaseDialog")
            }
        }
        binding.yourScoreText.text = AppObjectController.getFirebaseRemoteConfig().getString(
            GRAMMAR_TEST_COMPLETE_DESCRIPTION + PrefManager.getStringValue(
                CURRENT_COURSE_ID,
                false,
                DEFAULT_COURSE_ID
            )
        ).replace("\\n", "\n")

        binding.scoreStartBtn.text  =AppObjectController.getFirebaseRemoteConfig().getString(
            GRAMMAR_CONTINUE_BUTTON_TEXT + PrefManager.getStringValue(
                CURRENT_COURSE_ID,
                false,
                DEFAULT_COURSE_ID
            )
        )

        binding.continueBtn.text = AppObjectController.getFirebaseRemoteConfig().getString(
            GRAMMAR_CONTINUE_BUTTON_TEXT + PrefManager.getStringValue(
                CURRENT_COURSE_ID,
                false,
                DEFAULT_COURSE_ID
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (!PrefManager.getBoolValue(HAS_SEEN_GRAMMAR_ANIMATION))
            showGrammarAnimation()
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_GRAMMAR_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = View.GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (lessonNumber == 1) {
                    withContext(Dispatchers.Main) {
                        binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
                        binding.txtTooltipIndex.text = getString(
                            R.string._of_,
                            (currentTooltipIndex + 1),
                            lessonTooltipList.size
                        )
                        binding.lessonTooltipLayout.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    fun showGrammarAnimation() {
        try {
            animationJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        animationJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val overlayButtonItem = com.joshtalks.joshskills.common.ui.tooltip.TooltipUtils.getOverlayItemFromView(binding.startBtn)
                overlayButtonItem?.let {
                    grammarAnimationListener?.showGrammarAnimation(it)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < lessonTooltipList.size - 1) {
            currentTooltipIndex++
            binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
            binding.txtTooltipIndex.text =
                getString(R.string._of_, (currentTooltipIndex + 1), lessonTooltipList.size)
        } else {
            binding.lessonTooltipLayout.visibility = View.GONE
            PrefManager.put(HAS_SEEN_GRAMMAR_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = View.GONE
        PrefManager.put(HAS_SEEN_GRAMMAR_TOOLTIP, true)
    }

    private fun completeGrammarCardLogic() {
        lessonActivityListener?.onSectionStatusUpdate(
            if (PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)) TRANSLATION_POSITION else GRAMMAR_POSITION,
            true
        )
    }

    fun startOnlineExamTest() {
        MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_QUIZ_START)
            .addParam(ParamKeys.LESSON_ID, lessonId)
            .addParam(ParamKeys.LESSON_NUMBER, lessonNumber)
            .push()

        if (PermissionUtils.isStoragePermissionEnabled(AppObjectController.joshApplication).not()) {
            askStoragePermission()
            return
        }
        moveToOnlineTestFragment()
    }

    fun moveToOnlineTestFragment() {
        activity?.supportFragmentManager?.let { fragmentManager ->
            binding.parentContainer.visibility = View.VISIBLE
            binding.startTestContainer.visibility = View.GONE
            binding.testCompletedContainer.visibility = View.GONE
            binding.testScoreContainer.visibility = View.GONE
            if (PrefManager.hasKey(IS_A2_C1_RETENTION_ENABLED) && PrefManager.getStringValue(
                    CURRENT_COURSE_ID
                ) == DEFAULT_COURSE_ID
            ) {
                A2C1Impressions.saveImpression(A2C1Impressions.Impressions.START_LESSON_QUESTIONS)
                viewModel.postGoal("RULE_${lessonNumber}_STARTED", CampaignKeys.A2_C1.name)
            }
            fragmentManager
                .beginTransaction()
                .replace(
                    R.id.parent_Container,
                    OnlineTestFragment.getInstance(lessonNumber),
                    OnlineTestFragment.TAG
                )
                .addToBackStack(TAG)
                .commitAllowingStateLoss()
        }
    }

    private fun askStoragePermission() {
        PermissionUtils.storageReadAndWritePermission(
            requireContext(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            moveToOnlineTestFragment()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                            //errorDismiss()
                            return
                        }
                        return
                    }
                    report?.isAnyPermissionPermanentlyDenied?.let {
                        PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
                        //errorDismiss()
                        return
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

    private fun showGrammarCompleteLayout() {
        binding.parentContainer.visibility = View.GONE
        binding.startTestContainer.visibility = View.GONE
        if (PrefManager.hasKey(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                IS_FREE_TRIAL,
                isConsistent = false,
                defValue = false
            )
        ) {
            binding.testScoreContainer.visibility = View.VISIBLE
            if (scoreText != -1) {
                binding.score.text = getString(R.string.test_score, scoreText)

            } else {
                binding.score.text = getString(
                    R.string.test_score, PrefManager.getIntValue(
                        FREE_TRIAL_TEST_SCORE, false, 0
                    )
                )

            }
            if (pointsList.isNullOrBlank().not()) {
                showSnackBar(binding.rootView, Snackbar.LENGTH_LONG, pointsList)
                playSnackbarSound(requireContext())
            }
        } else {
            binding.testCompletedContainer.visibility = View.VISIBLE
            binding.confetti.playAnimation()
            binding.lottieAnimationView.playAnimation()
        }
    }

    fun onGrammarContinueClick() {
        MixPanelTracker.publishEvent(MixPanelEvent.GRAMMAR_CONTINUE)
            .addParam(ParamKeys.LESSON_ID, lessonId)
            .push()
        lessonActivityListener?.onNextTabCall(
            if (PrefManager.hasKey(IS_A2_C1_RETENTION_ENABLED) && PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED))
                TRANSLATION_POSITION
            else
                GRAMMAR_POSITION
        )
    }

    private fun initBottomMargin() {
        if (isAdded && activity is LessonActivity && (requireActivity() as LessonActivity).getBottomBannerHeight() > 0) {
            binding.linearLayout.addView(
                View(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (requireActivity() as LessonActivity).getBottomBannerHeight()
                    )
                }
            )
        }
    }

    companion object {
        const val TAG = "GrammarOnlineTestFragment"
        const val CURRENT_LESSON_NUMBER = "current_lesson_number"
        const val POINTS_LIST = "points_list"
        const val SCORE_TEXT = "score_text"
        const val HAS_COMPLETED_TEST = "has_completed_test"
        private var animationJob: Job? = null

        @JvmStatic
        fun getInstance(
            lessonNumber: Int,
            scoreText: Int? = null,
            pointsList: String? = null,
            hasCompletedTest:Boolean = false
        ): GrammarOnlineTestFragment {
            val args = Bundle()
            args.putInt(CURRENT_LESSON_NUMBER, lessonNumber)
            args.putString(POINTS_LIST, pointsList)
            scoreText?.let {
                args.putInt(SCORE_TEXT, scoreText)
            }
            args.putBoolean(HAS_COMPLETED_TEST, hasCompletedTest)
            val fragment = GrammarOnlineTestFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onTestCompleted() {
        if (PrefManager.getBoolValue(IS_FREE_TRIAL))
            viewModel.getCoursePopupData(PurchasePopupType.GRAMMAR_COMPLETED)
        showGrammarCompleteLayout()
    }

    fun getStartButtonText() = AppObjectController.getFirebaseRemoteConfig().getString(
        GRAMMAR_START_BUTTON_TEXT + PrefManager.getStringValue(
            CURRENT_COURSE_ID,
            false,
            DEFAULT_COURSE_ID
        )
    )

    fun getContinueButtonText() = AppObjectController.getFirebaseRemoteConfig().getString(
        GRAMMAR_CONTINUE_BUTTON_TEXT + PrefManager.getStringValue(
            CURRENT_COURSE_ID,
            false,
            DEFAULT_COURSE_ID
        )
    )

    fun getTestCompletedDescription() = AppObjectController.getFirebaseRemoteConfig().getString(
        GRAMMAR_TEST_COMPLETE_DESCRIPTION + PrefManager.getStringValue(
            CURRENT_COURSE_ID,
            false,
            DEFAULT_COURSE_ID
        )
    ).replace("\\n", "\n")

}

interface GrammarAnimation {
    fun showGrammarAnimation(overlayItem: TooltipUtils.ItemOverlay)
}