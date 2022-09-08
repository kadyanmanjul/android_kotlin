package com.joshtalks.joshskills.ui.lesson.speaking

import android.animation.TimeAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.lottie.LottieCompositionFactory
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.pstn_states.PSTNState
import com.joshtalks.joshskills.databinding.SpeakingPractiseFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.callWithExpert.CallWithExpertActivity
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.fpp.RecentCallActivity
import com.joshtalks.joshskills.ui.group.views.JoshVoipGroupActivity
import com.joshtalks.joshskills.ui.lesson.*
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.constant.State
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import java.time.Duration
import java.util.*

const val NOT_ATTEMPTED = "NA"
const val COMPLETED = "CO"
const val ATTEMPTED = "AT"
const val UPGRADED_USER = "NFT"
const val BLOCKED = "BLOCKED"
const val SHOW_WARNING_POPUP = "SHOW_WARNING_POPUP"

class SpeakingPractiseFragment : CoreJoshFragment() {

    private lateinit var binding: SpeakingPractiseFragmentBinding
    var lessonActivityListener: LessonActivityListener? = null
    private var compositeDisposable = CompositeDisposable()
    private var courseId: String = EMPTY
    private var topicId: String? = EMPTY
    private var questionId: String? = null
    private var haveAnyFavCaller = false
    private var isAnimationShown = false
    private var LEVEL_INCREMENT = 10
    private val MAX_LEVEL = 1000
    private var mAnimator: TimeAnimator? = null
    private var mCurrentLevel = 0
    private var mClipDrawable: ClipDrawable? = null
    private var beforeAnimation: GradientDrawable? = null
    private var lessonNo = 0
    private var beforeTwoMinTalked = -1
    private var afterTwoMinTalked = -1
    private val twoMinutes: Int = 2
    private val fiveMinutes: Int = 5
    private val tenMinutes: Int = 10
    private val twentyMinutes: Int = 20
    private var isTwentyMinFtuCallActive = false
    private var isIntroVideoEnabled = false
    private var lessonID = -1
    private var isBlockedFT = false
    private var countdownTimerBack: CountdownTimerBack? = null
    private val event = EventLiveData
    private val getBlockStatus = fun(){


    }



    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    private var currentTooltipIndex = 0
    private val lessonTooltipList by lazy {
        listOf(
            "कोर्स का सबसे मज़ेदार हिस्सा।",
            "यहाँ हम एक प्रैक्टिस पार्टनर के साथ निडर होकर इंग्लिश बोलने का अभ्यास करेंगे"
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.speaking_practise_fragment, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.vm = viewModel
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        binding.markAsCorrect.isVisible = BuildConfig.DEBUG
        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        binding.markAsCorrect.setOnClickListener { speakingSectionComplete() }
    }

    override fun onResume() {
        super.onResume()
        if (topicId.isNullOrBlank().not()) {
            viewModel.getTopicDetail(topicId!!)
        }
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT)) {
            viewModel.lessonSpotlightStateLiveData.postValue(null)
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                delay(100)
                viewModel.lessonSpotlightStateLiveData.postValue(LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2)
                PrefManager.put(HAS_SEEN_SPEAKING_SPOTLIGHT, true)
            }
        }
        viewModel.isFavoriteCallerExist()
        subscribeRXBus()
        //checkForVoipState()

    }

    private fun getVoipState(): State? {
        try {
            return requireActivity().getVoipState()
        } catch (ex: java.lang.Exception) {
            showToast("Please retry again later")
            ex.printStackTrace()
        }
        return null
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(DBInsertion::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({
                    viewModel.isFavoriteCallerExist()
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun addObservers() {
        viewModel.abTestRepository.apply {
            isTwentyMinFtuCallActive = isVariantActive(VariantKeys.TWENTY_MIN_ENABLED)
            isIntroVideoEnabled = isVariantActive(VariantKeys.SIV_ENABLED)
        }
        viewModel.lessonId.observe(viewLifecycleOwner) {
            lessonID = it
        }

        viewModel.blockLiveData.observe(viewLifecycleOwner) {
            when (it) {
              true->{
                  val durationInMillis = Duration.ofMinutes(viewModel.isUserBlock?.get()?.duration!!.toLong()).toMillis()
                  val unblockTimestamp = viewModel.isUserBlock?.get()?.timestamp?.plus(durationInMillis)
                  val currentTimestamp = System.currentTimeMillis()
                  startTimer(currentTimestamp- (unblockTimestamp!!))
              }
            }
        }
        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner) {
            val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
            questionId = spQuestion?.id

            spQuestion?.topicId?.let {
                this.topicId = it
                viewModel.getTopicDetail(it)
            }
            spQuestion?.lessonId?.let { viewModel.getCourseIdByLessonId(it) }
        }
        viewModel.lessonSpotlightStateLiveData.observe(requireActivity()) {
            when (it) {
                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    binding.nestedScrollView.scrollTo(0, binding.nestedScrollView.bottom)
                }
            }
        }
        viewModel.courseId.observe(viewLifecycleOwner) {
            courseId = it
        }
        binding.btnStartTrialText.setOnSingleClickListener {
            if (PrefManager.getBoolValue(IS_LOGIN_VIA_TRUECALLER))
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
            MarketingAnalytics.callInitiated()
            MixPanelTracker.publishEvent(MixPanelEvent.CALL_PRACTICE_PARTNER)
                .addParam(ParamKeys.LESSON_ID, lessonID)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                .addParam(ParamKeys.VIA, "speaking screen")
                .push()
            viewModel.postGoal(GoalKeys.CALL_PP_CLICKED.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
            val state = getVoipState()
            Log.d(TAG, " Start Call Button - Voip State $state")
            if (state == State.IDLE) {
                if (checkPstnState() == PSTNState.Idle) {
                    if (Utils.isInternetAvailable().not()) {
                        showToast("Seems like you have no internet")
                        return@setOnSingleClickListener
                    }
                    startPractise()
                } else {
                    showToast("Cannot make this call while on another call")
                }
            } else
                showToast("Wait for last call to get disconnected")
        }

        binding.btnGroupCall.setOnClickListener {
            if (PrefManager.getBoolValue(IS_LOGIN_VIA_TRUECALLER))
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
            if (getVoipState() == State.IDLE) {
                val intent = Intent(requireActivity(), JoshVoipGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, getConversationId())
                }
                startActivity(intent)
                MixPanelTracker.publishEvent(MixPanelEvent.CALL_PP_FROM_GROUP_LESSON)
                    .addParam(ParamKeys.LESSON_ID, lessonID)
                    .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                    .push()
            } else {
                showToast("Wait for last call to get disconnected")
            }
        }

        viewModel.speakingSpotlightClickLiveData.observe(viewLifecycleOwner) {
            val state = getVoipState()
            Log.d(TAG, " Start Call Button - Voip State $state")
            if (state == State.IDLE) {
                if (checkPstnState() == PSTNState.Idle) {
                    if (Utils.isInternetAvailable().not()) {
                        showToast("Seems like you have no internet")
                        return@observe
                    }
                    startPractise()
                } else {
                    showToast("Cannot make this call while on another call")
                }
            } else
                showToast("Wait for last call to get disconnected")
        }

        binding.btnContinue.setOnClickListener {
            lessonActivityListener?.onNextTabCall(
                SPEAKING_POSITION.minus(
                    if (PrefManager.getBoolValue(
                            IS_A2_C1_RETENTION_ENABLED
                        )
                    ) 0
                    else 1
                )
            )
            MixPanelTracker.publishEvent(MixPanelEvent.SPEAKING_CONTINUE)
                .addParam(ParamKeys.LESSON_ID, lessonID)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                .push()
        }
        binding.imgRecentCallsHistory.setOnSingleClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.VIEW_RECENT_CALLS).push()
            RecentCallActivity.openRecentCallActivity(
                requireActivity(),
                CONVERSATION_ID
            )
        }
        // redirect to buy screen
        binding.txtBuyToContinueCalls.setOnClickListener {
            activity?.let { it1 ->
                FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                    it1,
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                    )
                )
            }
        }

        viewModel.speakingTopicLiveData.observe(viewLifecycleOwner) { response ->
            binding.progressView.visibility = GONE
            if (response == null) {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            } else {
                try {
                    if (response.alreadyTalked < twoMinutes) {
                        beforeTwoMinTalked = 0
                        afterTwoMinTalked = 0
                    } else if (response.alreadyTalked >= twoMinutes) {
                        beforeTwoMinTalked = afterTwoMinTalked
                        afterTwoMinTalked = 1
                        viewModel.postGoal(GoalKeys.CALL_2MIN_COMPLETE.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    }
                    if (response.alreadyTalked >= fiveMinutes)
                        viewModel.postGoal(GoalKeys.CALL_5MIN_COMPLETE.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    if (response.alreadyTalked >= tenMinutes)
                        viewModel.postGoal(GoalKeys.CALL_10MIN_COMPLETE.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)
                    if (response.alreadyTalked >= twentyMinutes)
                        viewModel.postGoal(GoalKeys.CALL_20MIN_COMPLETE.NAME, CampaignKeys.ENGLISH_FOR_GOVT_EXAM.NAME)

                    if (beforeTwoMinTalked == 0 && afterTwoMinTalked == 1 && topicId != null && topicId == LESSON_ONE_TOPIC_ID && PrefManager.getBoolValue(
                            IS_FREE_TRIAL_CAMPAIGN_ACTIVE
                        )
                    ) {
                        viewModel.postGoal(
                            GoalKeys.EFT_GT_2MIN.name,
                            CampaignKeys.EXTEND_FREE_TRIAL.name
                        )
                        PrefManager.put(IS_FREE_TRIAL_CAMPAIGN_ACTIVE, false)
                    }

                    if (!PrefManager.getBoolValue(IS_FIRST_TIME_SPEAKING_SCREEN, defValue = false) && PrefManager.getBoolValue(
                            IS_FREE_TRIAL
                        )
                    ) {
                        binding.imgRecentCallsHistory.visibility = INVISIBLE
                        PrefManager.put(IS_FIRST_TIME_SPEAKING_SCREEN, true)
                    }

                    when (response.isFtCallerBlocked) {
                        BLOCKED -> {
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                PrefManager.put(IS_FREE_TRIAL_CALL_BLOCKED, value = true)
                                binding.containerReachedFtLimit.visibility = VISIBLE
                                binding.btnStartTrialText.isEnabled = false
                                binding.btnStartTrialText.alpha = 0.2F
                                binding.infoContainer.visibility = INVISIBLE
                                isBlockedFT = true
                            }
                        }
                        SHOW_WARNING_POPUP -> {
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL) && PrefManager.getBoolValue(HAS_SEEN_WARNING_POPUP_FT)
                                    .not()
                            ) {
                                // dialog for warning about shorter calls
                                binding.containerReachedFtLimit.visibility = GONE
                                AlertDialog.Builder(activity).setTitle(R.string.warning)
                                    .setIcon(R.drawable.ic_warning)
                                    .setPositiveButton(R.string.got_it) { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .setMessage(R.string.shorter_calls_error_message)
                                    .show()
                                PrefManager.put(HAS_SEEN_WARNING_POPUP_FT, true)
                            }

                        }
                        else -> {
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                binding.infoContainer.visibility = VISIBLE
                                binding.containerReachedFtLimit.visibility = GONE
                            }
                        }
                    }

                    binding.tvTodayTopic.text = response.topicName

                    if (!isTwentyMinFtuCallActive || response.callDurationStatus == UPGRADED_USER) {
                        PrefManager.put(
                            REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL, true
                        )
                        binding.tvPractiseTime.text =
                            response.alreadyTalked.toString().plus(" / ")
                                .plus(response.duration.toString())
                                .plus("\n Minutes")
                        binding.progressBar.progress = response.alreadyTalked.toFloat()
                        binding.progressBar.progressMax = response.duration.toFloat()

                        binding.textView.text = if (response.duration >= 10) {
                            getString(R.string.pp_messages, response.duration.toString())
                        } else {
                            getString(R.string.pp_message, response.duration.toString())
                        }
                    } else {
                        if (binding.txtHowToSpeak.visibility == VISIBLE) {
                            val layoutParams: ConstraintLayout.LayoutParams =
                                binding.txtHowToSpeak.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams.topToBottom = binding.ftuTwentyMinStatus.id
                        }

                        val layoutParams: ConstraintLayout.LayoutParams =
                            binding.infoContainer.layoutParams as ConstraintLayout.LayoutParams
                        if (binding.txtHowToSpeak.visibility == VISIBLE) layoutParams.topToBottom =
                            binding.txtHowToSpeak.id
                        else {
                            layoutParams.topToBottom = binding.ftuTwentyMinStatus.id
                        }

                        binding.ftuTwentyMinStatus.visibility = VISIBLE
                        binding.twentyMinFtuText.visibility = VISIBLE
                        binding.textView.visibility = GONE
                        binding.tvPractiseTime.visibility = View.INVISIBLE
                        binding.imageView.visibility = GONE
                        binding.infoContainer.backgroundTintList = null
                        postSpeakingScreenSeenGoal()
                        when (response.callDurationStatus) {
                            NOT_ATTEMPTED -> {
                                binding.ftuTwentyMinStatus.pauseAnimation()
                                binding.twentyMinFtuText.text =
                                    getString(R.string.twenty_min_call_target)
                                showTwentyMinAnimation("lottie/not_attempted.json")
                                binding.ftuTwentyMinStatus.setMinAndMaxProgress(0.0f, 0.7f)
                            }
                            COMPLETED -> {
                                binding.ftuTwentyMinStatus.pauseAnimation()
                                binding.twentyMinFtuText.text =
                                    getString(R.string.twenty_min_call_completed)
                                showTwentyMinAnimation("lottie/twenty_min_call_completed.json")
                                if (PrefManager.getBoolValue(TWENTY_MIN_CALL_GOAL_POSTED)
                                        .not() && PrefManager.getBoolValue(CALL_BTN_CLICKED)
                                ) {
                                    viewModel.postGoal(
                                        GoalKeys.TWENTY_MIN_CALL.NAME,
                                        CampaignKeys.TWENTY_MIN_TARGET.NAME
                                    )
                                    PrefManager.put(TWENTY_MIN_CALL_GOAL_POSTED, true)
                                }
                            }
                            ATTEMPTED -> {
                                binding.ftuTwentyMinStatus.pauseAnimation()
                                binding.twentyMinFtuText.text =
                                    getString(R.string.twenty_min_call_incomplete)
                                showTwentyMinAnimation("lottie/twenty_call_min_missed.json")
                                if (PrefManager.getBoolValue(
                                        TWENTY_MIN_CALL_ATTEMPTED_GOAL_POSTED
                                    )
                                        .not() && PrefManager.getBoolValue(CALL_BTN_CLICKED)
                                ) {
                                    viewModel.postGoal(
                                        GoalKeys.CALL_ATTEMPTED.name,
                                        CampaignKeys.TWENTY_MIN_TARGET.NAME
                                    )
                                    PrefManager.put(TWENTY_MIN_CALL_ATTEMPTED_GOAL_POSTED, true)
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                binding.groupTwo.visibility = VISIBLE
                if ((!isTwentyMinFtuCallActive || response.callDurationStatus == UPGRADED_USER) && response.alreadyTalked.toFloat() >= response.duration.toFloat()) {
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.tvPractiseTime.visibility = GONE
                    binding.progressBarAnim.visibility = VISIBLE
                    if (!isAnimationShown) {
                        binding.progressBarAnim.playAnimation()
                        isAnimationShown = true
                    }
                }

                if (isTwentyMinFtuCallActive && response.callDurationStatus != UPGRADED_USER) binding.progressBar.visibility =
                    View.INVISIBLE

                val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
                if (points.isNotEmpty()) {
                    // showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                    PrefManager.put(SPEAKING_POINTS, EMPTY)
                }

                if (isTwentyMinFtuCallActive && response.callDurationStatus == COMPLETED) {
                    speakingSectionComplete()
                } else if ((!isTwentyMinFtuCallActive || response.callDurationStatus == UPGRADED_USER) && response.alreadyTalked >= response.duration && response.isFromDb.not()) {
                    speakingSectionComplete()
                } else {
                    //binding.btnStart.playAnimation()
                }

                if (response.isNewStudentCallsActivated) {
                    binding.txtLabelNewStudentCalls.visibility = VISIBLE
                    binding.progressNewStudentCalls.visibility = VISIBLE
                    binding.progressNewStudentCalls.progress = response.totalNewStudentCalls
                    binding.progressNewStudentCalls.max = response.requiredNewStudentCalls
                    binding.txtProgressCount.visibility = VISIBLE
                    binding.txtProgressCount.text =
                        "${response.totalNewStudentCalls}/${response.requiredNewStudentCalls}"
                    binding.txtCallsLeft.visibility = VISIBLE
                    binding.txtCallsLeft.text = when (val dayOfWeek =
                        Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY ->
                            "1 day left"
                        else -> {
                            "${7 - (dayOfWeek - 1)} days left"
                        }
                    }
                    binding.txtLabelBecomeSeniorStudent.paintFlags =
                        binding.txtLabelBecomeSeniorStudent.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    binding.txtLabelBecomeSeniorStudent.visibility = VISIBLE
                    binding.btnNewStudent.visibility = VISIBLE
                    binding.infoContainer.visibility = GONE
                } else {
                    binding.txtLabelNewStudentCalls.visibility = GONE
                    binding.progressNewStudentCalls.visibility = GONE
                    binding.txtProgressCount.visibility = GONE
                    binding.txtCallsLeft.visibility = GONE
                    binding.txtLabelBecomeSeniorStudent.visibility = GONE
                    binding.btnNewStudent.visibility = GONE
                    if (!isBlockedFT) {
                        binding.infoContainer.visibility = VISIBLE
                    }
                }
            }
        }
        binding.btnFavorite.setOnClickListener {
            FavoriteListActivity.openFavoriteCallerActivity(
                requireActivity(),
                CONVERSATION_ID
            )
            viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
//            if (haveAnyFavCaller) {
//                startPractise(favoriteUserCall = true)
//            } else {
//                showToast(getString(R.string.empty_favorite_list_message))
//            }
            MixPanelTracker.publishEvent(MixPanelEvent.CALL_FAV_PRACTICE_PARTNER)
                .addParam(ParamKeys.LESSON_ID, lessonID)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                .push()
        }
        binding.btnNewStudent.setOnClickListener {

            MixPanelTracker.publishEvent(MixPanelEvent.CALL_NEW_STUDENT)
                .addParam(ParamKeys.LESSON_ID, lessonID)
                .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                .push()
            if (getVoipState() == State.IDLE)
                startPractise(favoriteUserCall = false, isNewUserCall = true)
            else
                showToast("Wait for last call to get disconnected")
        }

        if (AppObjectController.getFirebaseRemoteConfig().getBoolean(IS_CALL_WITH_EXPERT_ENABLED) && PrefManager.getStringValue(
                CURRENT_COURSE_ID
            ) == DEFAULT_COURSE_ID
        ) {
            binding.btnCallWithExpert.isVisible = true
        }

        binding.btnCallWithExpert.setOnClickListener {
            viewModel.saveMicroPaymentImpression(OPEN_EXPERT, previousPage = SPEAKING_PAGE)
            if (User.getInstance().isVerified) {
                Intent(requireActivity(), CallWithExpertActivity::class.java).also {
                    startActivity(it)
                }
            } else {
                navigateToLoginActivity()
            }
        }

        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }

        viewModel.lessonLiveData.observe(viewLifecycleOwner) {
            try {
                lessonNo = it?.lessonNo ?: 0

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModel.introVideoCompleteLiveData.observe(viewLifecycleOwner) {
            if (it == true) {
                binding.btnCallDemo.visibility = View.GONE
            }
        }
        initDemoViews(lessonNo)
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FLOW_FROM, "payment journey")
        }
        startActivity(intent)
        val broadcastIntent=Intent().apply {
            action = CALLING_SERVICE_ACTION
            putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
        }
        LocalBroadcastManager.getInstance(requireActivity()).sendBroadcast(broadcastIntent)
    }

    private fun postSpeakingScreenSeenGoal() {
        if (PrefManager.getBoolValue(SPEAKING_SCREEN_SEEN_GOAL_POSTED)
                .not() && PrefManager.getBoolValue(IS_SPEAKING_SCREEN_CLICKED)
        ) {
            viewModel.postGoal(GoalKeys.P2P_SCREEN_SEEN.name, CampaignKeys.TWENTY_MIN_TARGET.NAME)
            PrefManager.put(SPEAKING_SCREEN_SEEN_GOAL_POSTED, true)
        }
    }

    private fun showTwentyMinAnimation(jsonFileLottieAnimation: String) {
        LottieCompositionFactory.fromAsset(requireContext(), jsonFileLottieAnimation)
            .addListener {
                binding.ftuTwentyMinStatus.setComposition(it)
                binding.ftuTwentyMinStatus.resumeAnimation()
            }
    }

    private fun initDemoViews(it: Int) {
        if (it == 1 && isIntroVideoEnabled && PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID) {
            lessonActivityListener?.showIntroVideo()
            lessonNo = it
            binding.btnCallDemo.visibility = View.GONE
            binding.txtHowToSpeak.visibility = View.VISIBLE
            binding.txtHowToSpeak.setOnClickListener {
                lessonActivityListener?.introVideoCmplt()
                viewModel.isHowToSpeakClicked(true)
                binding.btnCallDemo.visibility = View.VISIBLE
                viewModel.saveIntroVideoFlowImpression(HOW_TO_SPEAK_TEXT_CLICKED)
                MixPanelTracker.publishEvent(MixPanelEvent.HOW_TO_SPEAK)
                    .addParam(ParamKeys.LESSON_ID, lessonID)
                    .addParam(ParamKeys.LESSON_NUMBER, lessonNo)
                    .push()
            }

            try {
                viewModel.callBtnHideShowLiveData.observe(viewLifecycleOwner) {
                    try {
                        if (it == 1) {
                            requireActivity().runOnUiThread {
                                binding.nestedScrollView.visibility = View.INVISIBLE
                                binding.btnCallDemo.visibility = View.VISIBLE
                            }
                        }
                        if (it == 2) {
                            requireActivity().runOnUiThread {
                                binding.nestedScrollView.visibility = View.VISIBLE
                                binding.btnCallDemo.visibility = View.GONE
                            }
                        }
                    } catch (ex: Exception) {
                    }
                }
            } catch (ex: Exception) {
            }
        } else {
            binding.btnCallDemo.visibility = View.GONE
        }
        binding.btnGroupCall.isVisible =
            PrefManager.getBoolValue(IS_COURSE_BOUGHT) && PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID

        binding.btnFavorite.isVisible =
            PrefManager.getBoolValue(IS_COURSE_BOUGHT)
    }

    private fun speakingSectionComplete() {
        binding.btnContinue.visibility = VISIBLE
//        binding.btnStart.pauseAnimation()
//        binding.btnContinue.playAnimation()
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            questionId
        )
        lessonActivityListener?.onSectionStatusUpdate(SPEAKING_POSITION, true)
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (viewModel.lessonLiveData.value?.lessonNo == 1) {
                    withContext(Dispatchers.Main) {
                        binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
                        binding.txtTooltipIndex.text =
                            "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
                        binding.lessonTooltipLayout.visibility = VISIBLE
                    }
                }
            }
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < lessonTooltipList.size - 1) {
            currentTooltipIndex++
            binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
            binding.txtTooltipIndex.text =
                "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
        } else {
            binding.lessonTooltipLayout.visibility = GONE
            PrefManager.put(HAS_SEEN_SPEAKING_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = GONE
        PrefManager.put(HAS_SEEN_SPEAKING_TOOLTIP, true)
    }

    private fun startPractise(
        favoriteUserCall: Boolean = false,
        isNewUserCall: Boolean = false,
    ) {
        viewModel.saveImpression(P2P_CALL_BUTTON_CLICK)
        PrefManager.put(CALL_BTN_CLICKED, true)
        if (isAdded && activity != null) {
            if (PermissionUtils.isCallingPermissionEnabled(requireContext())) {
                startPracticeCall()
                return
            }
        }
        if (isAdded && activity != null) {
            PermissionUtils.callingFeaturePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (report.isAnyPermissionPermanentlyDenied) {
                                if (isAdded && activity != null) {
                                    viewModel.saveImpression(CALL_PERMISSION_DENIED)
                                    PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                        requireActivity(),
                                        message = R.string.call_start_permission_message
                                    )
                                    return
                                } else {
                                    showToast(getString(R.string.something_went_wrong))
                                }
                            }
                            if (flag) {
                                startPracticeCall()
                                return
                            } else {
                                viewModel.saveImpression(CALL_PERMISSION_DENIED)
                                MaterialDialog(requireActivity()).show {
                                    message(R.string.call_start_permission_message)
                                    positiveButton(R.string.ok)
                                }
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        token?.continuePermissionRequest()
                    }
                }
            )
        }
    }

    fun openNetworkDialog(v: View) {
        if (isAdded && activity != null) {
            val dialog = AlertDialog.Builder(context)
            dialog
                .setMessage(getString(R.string.network_message))
                .setPositiveButton("GOT IT")
                { dialog, _ -> dialog.dismiss() }.show()
        }
    }

    fun openBlockReasonDialog(v: View) {
        if (isAdded && activity != null) {
            val dialog = AlertDialog.Builder(context)
            dialog
                .setMessage(viewModel.isUserBlock.get()?.reason?.let {
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(it).replace("<br\\>", "\n")
                })
                .setPositiveButton("GOT IT")
                { dialog, _ ->
                    viewModel.isUserCallBlock(true)
                    dialog.dismiss()
                }.show()
        }
    }

    fun openRatingDialog(v: View) {
        if (isAdded && activity != null) {
            val rating = PrefManager.getRatingObject(RATING_OBJECT)?.rating
            val dialog = AlertDialog.Builder(context)
            dialog
                .setTitle(getString(R.string.rating_title, rating.toString()))
                .setMessage(getString(R.string.rating_message))
                .setPositiveButton("GOT IT")
                { dialog, _ ->  }.show()

        }
    }

    fun animateButton() {
        mCurrentLevel = 0
        mAnimator?.start()
    }

    fun fillButton() {
        mCurrentLevel = MAX_LEVEL
        mAnimator?.start()
    }

    private fun startPracticeCall() {
        if (isAdded && activity != null) {
            PrefManager.increaseCallCount()
            if (PrefManager.getCallCount() == 3)
                viewModel.getRating()

            val callIntent = Intent(requireActivity(), VoiceCallActivity::class.java)
            callIntent.apply {
                putExtra(INTENT_DATA_COURSE_ID, courseId)
                putExtra(INTENT_DATA_TOPIC_ID, topicId)
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
            }
            startActivity(callIntent)
        }
    }

    fun showSeniorStudentScreen() {
        SeniorStudentActivity.startSeniorStudentActivity(requireActivity())
    }
    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack?.stop()
        countdownTimerBack = object : CountdownTimerBack((startTimeInMilliSeconds)) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    binding.cdTimer.text = UtilTime.timeFormatted(millis)
                }
            }

            override fun onTimerFinish() {
              viewModel.blockLiveData.postValue(false)
                PrefManager.putPrefObject(BLOCK_STATUS, BlockStatusModel(0,0,"",0))

            }
        }
        countdownTimerBack?.startTimer()
    }
    companion object {
        @JvmStatic
        fun newInstance() =
            SpeakingPractiseFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
    }
}
