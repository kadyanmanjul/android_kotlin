package com.joshtalks.joshskills.ui.lesson.speaking

import android.animation.TimeAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.afollestad.materialdialogs.MaterialDialog
import com.airbnb.lottie.LottieCompositionFactory
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.SPEAKING_BB_TIP_BUTTON_CONTENT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.SPEAKING_BB_TIP_BUTTON_HEADER
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.SPEAKING_BB_TIP_TOPIC_HEADER
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.pstn_states.PSTNState
import com.joshtalks.joshskills.databinding.SpeakingPractiseFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.callWithExpert.CallWithExpertActivity
import com.joshtalks.joshskills.ui.extra.setOnShortSingleClickListener
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.fpp.RecentCallActivity
import com.joshtalks.joshskills.ui.group.views.JoshVoipGroupActivity
import com.joshtalks.joshskills.ui.lesson.*
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.tooltip.TooltipUtils
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.util.UtilTime
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
    private lateinit var toolTipTopicContainer: Balloon
    private lateinit var toolTipButton: Balloon
    private lateinit var toolTipRecentCall:Balloon
    private var howToSpeakButtonShow = EMPTY

    private val getBlockStatus = fun() {
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
        binding.lifecycleOwner = requireActivity()
        binding.handler = this
        binding.vm = viewModel
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        binding.markAsCorrect.isVisible = BuildConfig.DEBUG
        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        initBottomMargin()
        binding.markAsCorrect.setOnClickListener { speakingSectionComplete() }
    }

    override fun onResume() {
        super.onResume()
        if (topicId.isNullOrBlank().not()) {
            viewModel.getTopicDetail(topicId!!)
        }
        viewModel.isFavoriteCallerExist()
        subscribeRXBus()
        //checkForVoipState()
    }

    private fun getVoipState(): State {
        return com.joshtalks.joshskills.voip.data.local.PrefManager.getVoipState()
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

        viewModel.speakingLiveData.observe(viewLifecycleOwner) {
            binding.welcomeContainer.visibility = GONE
            binding.btnPeerToPeerCall.visibility = VISIBLE
            dismissTooltipButton()
        }

        viewModel.blockLiveData.observe(viewLifecycleOwner) {
            when (it) {
                true -> {
                    val durationInMillis =
                        Duration.ofMinutes(viewModel.isUserBlock.get()?.duration!!.toLong()).toMillis()
                    val unblockTimestamp = viewModel.isUserBlock.get()?.timestamp?.plus(durationInMillis)
                    val currentTimestamp = System.currentTimeMillis()
                    startTimer(currentTimestamp - (unblockTimestamp!!))
                }
                false -> {
                    Log.d(TAG, "checkBlockStatus:blockStatusFromApi 4")
                    binding.blockContainer.visibility = View.GONE
                }
            }
        }
        if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
            viewModel.callCountLiveData.observe(viewLifecycleOwner) {
                it?.let {
                    binding.infoContainer.visibility = GONE
                    binding.txtLabelCallsLeft.visibility = VISIBLE
                    binding.txtLabelCallsLeft.paintFlags =
                        binding.txtLabelCallsLeft.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    binding.txtLabelCallsLeft.text = "$it calls left"
                    binding.txtLabelCallsLeft.setOnClickListener {
                        viewModel.getCoursePopupData(PurchasePopupType.SPEAKING_COMPLETED)
                    }
                    if (it == 0) {
                        val text = AppObjectController.getFirebaseRemoteConfig().getString(
                            FirebaseRemoteConfigKey.BUY_COURSE_SPEAKING_TOOLTIP.plus(
                                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }
                            )
                        )
                        if (text.isBlank()) {
                            binding.bbTooltipGroup.visibility = GONE
                        } else {
                            binding.bbTooltipGroup.visibility = VISIBLE
                            binding.layoutBbTip.findViewById<MaterialTextView>(R.id.balloon_text).text = text
                        }
                        binding.bbTooltipGroup.visibility = VISIBLE
                        binding.txtLabelCallsLeft.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.critical
                            )
                        )
                        binding.blockContainer.visibility = GONE
                    } else if (it <= 3) {
                        binding.blockContainer.visibility = GONE
                        val text = AppObjectController.getFirebaseRemoteConfig().getString(
                            FirebaseRemoteConfigKey.BUY_COURSE_SPEAKING_TOOLTIP.plus(
                                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }
                            )
                        )
                        if (text.isBlank()) {
                            binding.bbTooltipGroup.visibility = GONE
                        } else {
                            binding.bbTooltipGroup.visibility = VISIBLE
                            binding.layoutBbTip.findViewById<MaterialTextView>(R.id.balloon_text).text = text
                        }
                        binding.txtLabelCallsLeft.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.primary_500
                            )
                        )
                    } else {
                        binding.bbTooltipGroup.visibility = GONE
                    }
                }
            }
        } else {
            binding.txtLabelCallsLeft.visibility = GONE
            binding.bbTooltipGroup.visibility = GONE
        }

        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner) {
            val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
            questionId = spQuestion?.id

            spQuestion?.topicId?.let {
                this.topicId = it
                Log.d("SAGAR", "addObservers() called")
                viewModel.getTopicDetail(it)
            }
            spQuestion?.lessonId?.let { viewModel.getCourseIdByLessonId(it) }
        }
        viewModel.lessonSpotlightStateLiveData.observe(requireActivity()) {
            when (it) {
                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    binding.nestedScrollView.scrollTo(0, binding.nestedScrollView.bottom)
                }
                else -> {}
            }
        }
        viewModel.courseId.observe(viewLifecycleOwner) {
            courseId = it
        }
        binding.btnPeerToPeerCall.setOnSingleClickListener {
            if (!PrefManager.getBoolValue(IS_FIRST_TIME_CALL_INITIATED) && PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                MarketingAnalytics.callInitiatedForFirstTime()
                PrefManager.put(IS_FIRST_TIME_CALL_INITIATED, true)
            }
            if (viewModel.callCountLiveData.value == 0) {
                viewModel.getCoursePopupData(PurchasePopupType.SPEAKING_COMPLETED)
                return@setOnSingleClickListener
            }
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
            if (binding.imgRecentCallsHistory.text == "How to speak"){
                lessonActivityListener?.introVideoCmplt()
                lessonActivityListener?.showIntroVideo()
                //viewModel.isHowToSpeakClicked(true)
                binding.btnCallDemo.visibility = View.VISIBLE
                viewModel.saveIntroVideoFlowImpression(HOW_TO_SPEAK_TEXT_CLICKED)
            }else{
                RecentCallActivity.openRecentCallActivity(
                    requireActivity(),
                    CONVERSATION_ID
                )
            }
        }
        // redirect to buy screen
        binding.txtBuyToContinueCalls.setOnClickListener {
            startBuyPageActivity(it)
        }

        viewModel.speakingTooltipLiveData.observe(viewLifecycleOwner){ response ->
            showToolTipOnLesson(response)
        }

        viewModel.speakingTopicLiveData.observe(viewLifecycleOwner) { response ->
            Log.d("SAGAR", "addObservers() called with: response = $response")
            binding.progressView.visibility = GONE
            viewModel.isNewStudentActive.set(response?.isNewStudentCallsActivated)
            howToSpeakButtonShow = response?.howToTalkButton ?: EMPTY
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

                    if (beforeTwoMinTalked == 0 && afterTwoMinTalked == 1 && topicId != null &&
                        topicId == LESSON_ONE_TOPIC_ID && PrefManager.getBoolValue(IS_FREE_TRIAL_CAMPAIGN_ACTIVE)
                    ) {
                        viewModel.postGoal(
                            GoalKeys.EFT_GT_2MIN.name,
                            CampaignKeys.EXTEND_FREE_TRIAL.name
                        )
                        PrefManager.put(IS_FREE_TRIAL_CAMPAIGN_ACTIVE, false)
                    }

                    when (response.isFtCallerBlocked) {
                        BLOCKED -> {
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                                PrefManager.put(IS_FREE_TRIAL_CALL_BLOCKED, value = true)
                                binding.containerReachedFtLimit.visibility = VISIBLE
                                binding.btnPeerToPeerCall.isEnabled = false
                                binding.btnPeerToPeerCall.alpha = 0.2F
                                binding.infoContainer.visibility = GONE
                                isBlockedFT = true
                            }
                        }
                        SHOW_WARNING_POPUP -> {
                            if (PrefManager.getBoolValue(IS_FREE_TRIAL) && PrefManager.getBoolValue(
                                    HAS_SEEN_WARNING_POPUP_FT
                                )
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
                      //  PrefManager.put(REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL, true)
                        binding.tvPractiseTime.text =
                            response.alreadyTalked.toString().plus("/")
                                .plus(response.duration.toString())
                                .plus("\n Minutes")
                        binding.progressBar.progress = response.alreadyTalked.toFloat()
                        binding.progressBar.progressMax = response.duration.toFloat()

                        binding.infoContainer.findViewById<AppCompatTextView>(R.id.info_text_subheading).text =
                            response.speakingInfoText?.let {
                                it.ifBlank { getInfoTipText(response.duration) }
                            } ?: getInfoTipText(response.duration)

                        binding.spTitle.text = response.speakingTabTitle?.let {
                            it.ifBlank { getString(R.string.speak_practise_title) }
                        } ?: getString(R.string.speak_practise_title)

                        binding.btnPeerToPeerCall.text = response.p2pBtnText?.let {
                            it.ifBlank { getString(R.string.call_practice_partner) }
                        } ?: getString(R.string.call_practice_partner)

                        if (!response.p2pBtnIcon.isNullOrBlank())
                            binding.btnPeerToPeerCall.icon = requireActivity().getDrawable(R.drawable.ic_phone_icon)

                        //TODO get call duration so we can show overlay on how to talk

                        // agar p2p button wale tootlip enable hai tu un 3 tooltip dikhne ke bad ye ayega tu vo bhi check karna hoga vo enable hai ya nahi varna
                        // ye check karna hai how to speak enable hai tu direct ye dikha du
                        if(viewModel.abTestRepository.isVariantActive(VariantKeys.SPEAKING_TOOLTIP_V2_ENABLED)){
                            if (((VoipPref.getLastCallDurationInSec() != 1L) && (VoipPref.getLastCallDurationInSec() <= (response.howToTalkButton?.toLong() ?: 0))) && PrefManager.getBoolValue(
                                    HAS_SEEN_SPEAKING_BUTOON_TOOLTIP) && !PrefManager.getBoolValue(HAS_SEEN_SPEAKING_RECENT_BUTTON_TOOLTIP)){
                                Log.e("sagar", "addObservers: Inside if", )
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID)
                                        setOverlayAnimationOnRecentCallButton()
                                }
                            }
                        }else{
                            if (((VoipPref.getLastCallDurationInSec() != 1L) && (VoipPref.getLastCallDurationInSec() <= (response.howToTalkButton?.toLong() ?: 0))) && !PrefManager.getBoolValue(HAS_SEEN_SPEAKING_RECENT_BUTTON_TOOLTIP)){
                                Log.e("sagar", "addObservers: Inside if", )
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID)
                                        setOverlayAnimationOnRecentCallButton()
                                }
                            }
                        }

                        requireActivity().findViewById<MaterialTextView>(R.id.spotlight_call_btn).text =
                            response.p2pBtnText?.let {
                                it.ifBlank { getString(R.string.call_practice_partner) }
                            } ?: getString(R.string.call_practice_partner)
                    } else {

                        binding.ftuTwentyMinStatus.visibility = VISIBLE
//                        binding.twentyMinFtuText.visibility = VISIBLE
//                        binding.textView.visibility = GONE
                        binding.tvPractiseTime.visibility = View.INVISIBLE
                        //binding.imageView.visibility = GONE
                        binding.infoContainer.backgroundTintList = null
                        postSpeakingScreenSeenGoal()
                        when (response.callDurationStatus) {
                            NOT_ATTEMPTED -> {
                                binding.ftuTwentyMinStatus.pauseAnimation()
                                //binding.twentyMinFtuText.text =
                                getString(R.string.twenty_min_call_target)
                                showTwentyMinAnimationFromUrl(getString(R.string.not_attempted_url))
                                binding.ftuTwentyMinStatus.setMinAndMaxProgress(0.0f, 0.7f)
                            }
                            COMPLETED -> {
                                binding.ftuTwentyMinStatus.pauseAnimation()
                                //   binding.twentyMinFtuText.text =
                                //  getString(R.string.twenty_min_call_completed)
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
//                                binding.twentyMinFtuText.text =
//                                    getString(R.string.twenty_min_call_incomplete)
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
                binding.spTitle.visibility = VISIBLE
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
                if (PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
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

        binding.btnCallWithExpert.setOnShortSingleClickListener {
            viewModel.saveMicroPaymentImpression(OPEN_EXPERT, previousPage = SPEAKING_PAGE)
            if (User.getInstance().isVerified) {
                Intent(requireActivity(), CallWithExpertActivity::class.java).also {
                    startActivity(it)
                }
            } else {
                navigateToLoginActivity()
            }
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

        if (viewModel.blockLiveData.value == false) {
            viewModel.isExpertBtnEnabled.observe(viewLifecycleOwner) {
                if (it && (PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID ||
                            PrefManager.getStringValue(CURRENT_COURSE_ID) == ENG_GOVT_EXAM_COURSE_ID)
                ) {
                    binding.btnCallWithExpert.isVisible = true
                }
            }
        } else {
            binding.btnCallWithExpert.isVisible = false
        }
        lifecycleScope.launch(Dispatchers.Main){
            initDemoViews(lessonNo)
        }
    }

    private fun showToolTipOnLesson(response: SpeakingTopic) {
        Log.e("sagar", "showToolTipOnLesson: " )
        viewModel.lessonSpotlightStateLiveData.postValue(null)
        if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_BB_TIP_SHOW) && PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT) && PrefManager.getBoolValue(HAS_SEEN_SPEAKING_BUTOON_TOOLTIP)) {
            viewModel.lessonSpotlightStateLiveData.postValue(null)
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                if (!PrefManager.getBoolValue(HAS_SEEN_SPEAKING_SPOTLIGHT)) {
                    //This code is for show balloon tooltip and highlight topic container
                    Log.e("sagar", "showToolTipOnLesson: 2")

                    setOverlayAnimationOnTopicContainer()
                    showTooltipTopic(
                        AppObjectController.getFirebaseRemoteConfig().getString(SPEAKING_BB_TIP_TOPIC_HEADER),
                        AppObjectController.getFirebaseRemoteConfig().getString(
                            FirebaseRemoteConfigKey.SPEAKING_BB_TIP_TOPIC_CONTENT.plus(
                                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }
                            )
                        )
                    )
                } else {
                    Log.e("sagar", "showToolTipOnLesson: 3" )
                    //This code is for show balloon tooltip and highlight peer to peer button
                    showTooltipButton(
                        AppObjectController.getFirebaseRemoteConfig().getString(SPEAKING_BB_TIP_BUTTON_HEADER),
                        AppObjectController.getFirebaseRemoteConfig().getString(
                            SPEAKING_BB_TIP_BUTTON_CONTENT.plus(
                                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }
                            )
                        )
                    )
                    binding.tapAnywhereToContinue.visibility = GONE
                    setOverlayAnimationOnSpeakingButton()
                }
            }
        }
    }

    fun getInfoTipText(duration: Int): String {
        return if (duration >= 10) {
            getString(R.string.pp_messages, duration.toString())
        } else {
            getString(R.string.pp_message, duration.toString())
        }
    }

    fun startBuyPageActivity(v: View) {
        activity?.let { it1 ->
//            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
//                it1,
//                AppObjectController.getFirebaseRemoteConfig().getString(
//                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
//                )
//            )
            BuyPageActivity.startBuyPageActivity(
                it1,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                ),
                "SPEAKING_BUY_TO_CALL"
            )
        }
    }

    private fun navigateToLoginActivity() {
        val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FLOW_FROM, "payment journey")
        }
        startActivity(intent)
        val broadcastIntent = Intent().apply {
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

    private fun showTwentyMinAnimationFromUrl(jsonUrl: String) {
        LottieCompositionFactory.fromUrl(requireContext(), jsonUrl)
            .addListener {
                binding.ftuTwentyMinStatus.setComposition(it)
                binding.ftuTwentyMinStatus.resumeAnimation()
            }
    }

    private fun initDemoViews(it: Int) {
        if (PrefManager.getBoolValue(IS_FREE_TRIAL) && lessonNo == 1) {
            if (PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID && howToSpeakButtonShow != EMPTY) {
                binding.imgRecentCallsHistory.visibility = VISIBLE
                binding.imgRecentCallsHistory.text = "How to speak"
                binding.imgRecentCallsHistory.icon =
                    requireActivity().getDrawable(R.drawable.info_iv)
            }
        } else if (PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
            binding.imgRecentCallsHistory.visibility = VISIBLE
        }

        if (it == 1 && isIntroVideoEnabled && PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID) {
            lessonActivityListener?.showIntroVideo()
            lessonNo = it
            binding.btnCallDemo.visibility = View.GONE

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
            if (PrefManager.getBoolValue(IS_COURSE_BOUGHT)){
                binding.imgRecentCallsHistory.visibility = VISIBLE
            }
            binding.btnCallDemo.visibility = View.GONE
        }

        if (viewModel.blockLiveData.value == false) {
            binding.btnGroupCall.isVisible =
                PrefManager.getBoolValue(IS_COURSE_BOUGHT) && PrefManager.getStringValue(
                    CURRENT_COURSE_ID
                ) == DEFAULT_COURSE_ID
        } else {
            binding.btnGroupCall.isVisible = false
        }

        viewModel.isFreeTrialUser.value = PrefManager.getBoolValue(IS_COURSE_BOUGHT)
    }

    private fun speakingSectionComplete() {
        binding.btnContinue.visibility = VISIBLE
        lessonActivityListener?.onQuestionStatusUpdate(
            QUESTION_STATUS.AT,
            questionId
        )
        lessonActivityListener?.onSectionStatusUpdate(SPEAKING_POSITION, true)
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
                                }
                            }
                            if (flag) {
                                startPracticeCall()
                                return
                            } else {
                                viewModel.saveImpression(CALL_PERMISSION_DENIED)
                                if (isAdded && activity != null) {
                                    MaterialDialog(requireActivity()).show {
                                        message(R.string.call_start_permission_message)
                                        positiveButton(R.string.ok)
                                    }
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
                    val newKey = it + "_" + PrefManager.getStringValue(CURRENT_COURSE_ID)
                    AppObjectController.getFirebaseRemoteConfig()
                        .getString(newKey).replace("<br\\>", "\n")
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
                { dialog, _ -> dialog.dismiss() }.show()
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
            VoipPref.resetAutoCallCount()
            requireActivity().startActivityForResult(callIntent, CALLING_ACTIVITY_REQUEST_CODE)
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
                PrefManager.putPrefObject(BLOCK_STATUS, BlockStatusModel(0, 0, "", 0))

            }
        }
        countdownTimerBack?.startTimer()
    }

    private fun initBottomMargin() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            withContext(Dispatchers.Main) {
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
        }
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

    private suspend fun setOverlayAnimationOnTopicContainer() {
        withContext(Dispatchers.Main) {
            val STATUS_BAR_HEIGHT = getStatusBarHeight()
            binding.welcomeContainer.visibility = VISIBLE
            val overlayImageView = binding.welcomeContainer.findViewById<ImageView>(R.id.welcome_item)
            val overlayItem = TooltipUtils.getOverlayItemFromView(binding.tooltipContainer)

            val layoutP : ViewGroup.MarginLayoutParams = overlayImageView.layoutParams as  ViewGroup.MarginLayoutParams
            layoutP.setMargins(resources.getDimension(R.dimen._10sdp).toInt(), 0, resources.getDimension(R.dimen._10sdp).toInt(), 0)
            overlayImageView.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(),R.drawable.round_rect_default_8))
            binding.tooltipContainer.requestLayout()

            PrefManager.put(HAS_SEEN_SPEAKING_SPOTLIGHT, true)
            viewModel.saveImpression(SPEAKING_TOOLTIP2)

            binding.welcomeContainer.setOnClickListener {
                Log.e("sagar", "showToolTipOnLesson: 4")
                binding.welcomeContainer.visibility = GONE
                dismissTooltipTopicContainer()
                //This code is for show balloon tooltip and highlight peer to peer button
                showTooltipButton(
                    AppObjectController.getFirebaseRemoteConfig().getString(SPEAKING_BB_TIP_BUTTON_HEADER),
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        SPEAKING_BB_TIP_BUTTON_CONTENT.plus(
                            PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID }
                        )
                    )
                )
                binding.tapAnywhereToContinue.visibility = GONE
                CoroutineScope(Dispatchers.Main).launch {
                    setOverlayAnimationOnSpeakingButton()
                }
            }

            overlayItem?.let {
                overlayImageView.setImageBitmap(it.viewBitmap)
                overlayImageView.x = 0F
                overlayImageView.y = it.y.toFloat() - STATUS_BAR_HEIGHT - resources.getDimension(R.dimen._32sdp) - resources.getDimension(R.dimen._30sdp)
                overlayImageView.requestLayout()
            }
        }
    }

    private suspend fun setOverlayAnimationOnSpeakingButton() {
        withContext(Dispatchers.Main) {
            val STATUS_BAR_HEIGHT = getStatusBarHeight()
            binding.welcomeContainer.visibility = VISIBLE
            viewModel.isSpeakingButtonTooltipShown.set(true)
            val overlayImageView = binding.welcomeContainer.findViewById<ImageView>(R.id.welcome_item)
            val overlayItem = TooltipUtils.getOverlayItemFromView(binding.btnPeerToPeerCall)

            overlayImageView.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(),R.drawable.rounded_blue_rectangle_with_border))
            binding.btnPeerToPeerCall.visibility = INVISIBLE
            binding.btnPeerToPeerCall.requestLayout()

            binding.welcomeContainer.setOnClickListener {
//                binding.welcomeContainer.visibility = GONE
//                binding.btnPeerToPeerCall.visibility = VISIBLE
//                dismissTooltipButton()
            }

            overlayImageView.setOnClickListener {
                binding.welcomeContainer.visibility = GONE
                dismissTooltipButton()
                val state = getVoipState()
                Log.d(TAG, " Start Call Button - Voip State $state")
                if (state == State.IDLE) {
                    if (checkPstnState() == PSTNState.Idle) {
                        if (Utils.isInternetAvailable().not()) {
                            showToast("Seems like you have no internet")
                        }
                        startPractise()
                    } else {
                        showToast("Cannot make this call while on another call")
                    }
                } else
                    showToast("Wait for last call to get disconnected")
            }

            overlayItem?.let {
                overlayImageView.setImageBitmap(it.viewBitmap)
                overlayImageView.x = it.x.toFloat()
                overlayImageView.y = it.y.toFloat() - STATUS_BAR_HEIGHT - resources.getDimension(R.dimen._32sdp) - resources.getDimension(R.dimen._47sdp)
                overlayImageView.requestLayout()
            }
            PrefManager.put(HAS_SEEN_SPEAKING_BUTOON_TOOLTIP, true)
            viewModel.saveImpression(SPEAKING_TOOLTIP3)

        }
    }

    private suspend fun setOverlayAnimationOnRecentCallButton() {
        withContext(Dispatchers.Main) {
            val STATUS_BAR_HEIGHT = getStatusBarHeight()
            binding.welcomeContainer.visibility = VISIBLE
            val overlayImageView = binding.welcomeContainer.findViewById<ImageView>(R.id.welcome_item)
            val overlayItem = TooltipUtils.getOverlayItemFromView(binding.imgRecentCallsHistory)

            showTooltipRecentCallButton()


            binding.welcomeContainer.setOnClickListener {
                PrefManager.put(HAS_SEEN_SPEAKING_RECENT_BUTTON_TOOLTIP, true)
                binding.welcomeContainer.visibility = GONE
                dismissTooltipRecentButton()
            }

            overlayItem?.let {
                overlayImageView.setImageBitmap(it.viewBitmap)
                overlayImageView.x = it.x.toFloat()
                overlayImageView.y = it.y.toFloat() - STATUS_BAR_HEIGHT - resources.getDimension(R.dimen._40sdp) - resources.getDimension(R.dimen._55sdp)
                overlayImageView.requestLayout()
            }
            viewModel.saveImpression(HOW_TO_SPEAK_TOOLTIP)
        }
    }


    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        requireActivity().window.getDecorView().getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = requireActivity().window.findViewById<View>(Window.ID_ANDROID_CONTENT).getTop()
        val titleBarHeight = contentViewTop - statusBarHeight
        Log.d("sagar", "getStatusBarHeight: $titleBarHeight")
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }

    private fun showTooltipTopic(testHeader: String, testBody:String) {
        try {
            if (this::toolTipTopicContainer.isInitialized.not()) {
                toolTipTopicContainer = Balloon.Builder(requireContext())
                    .setLayout(R.layout.layout_speaking_button_tooltip)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setIsVisibleArrow(true)
                    .setBackgroundColorResource(R.color.surface_tip)
                    .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                    .setWidthRatio(0.75f)
                    .setArrowPosition(0.2f)
                    .setDismissWhenTouchOutside(false)
                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                    .setLifecycleOwner(this)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .build()
                val textViewTitle = toolTipTopicContainer.getContentView().findViewById<MaterialTextView>(R.id.title)
                textViewTitle.text = testHeader
                val textViewSubHeadingText = toolTipTopicContainer.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
                textViewSubHeadingText.text = testBody

                toolTipTopicContainer.showAlignTop(binding.tooltipContainer)
            }

        } catch (ex: Exception) {
            Log.d(TAG, "showBuyCourseTooltip: ${ex.message}")
        }
    }
    private fun dismissTooltipTopicContainer(){
        if (this::toolTipTopicContainer.isInitialized && toolTipTopicContainer.isShowing){
            toolTipTopicContainer.dismiss()
        }
    }

    private fun showTooltipButton(testHeader: String, testBody:String) {
        try {
            if (this::toolTipButton.isInitialized.not()) {
                toolTipButton = Balloon.Builder(requireContext())
                    .setLayout(R.layout.layout_speaking_button_tooltip)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setIsVisibleArrow(true)
                    .setBackgroundColorResource(R.color.surface_tip)
                    .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                    .setWidthRatio(0.75f)
                    .setArrowPosition(0.5f)
                    .setDismissWhenTouchOutside(false)
                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                    .setLifecycleOwner(this)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .build()
                val textViewTitle = toolTipButton.getContentView().findViewById<MaterialTextView>(R.id.title)
                textViewTitle.text = testHeader
                val textViewSubHeadingText = toolTipButton.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
                textViewSubHeadingText.text = testBody

                toolTipButton.showAlignTop(binding.btnPeerToPeerCall)
            }

        } catch (ex: Exception) {
            Log.d(TAG, "showBuyCourseTooltip: ${ex.message}")
        }
    }

    private fun dismissTooltipButton(){
        if (this::toolTipButton.isInitialized && toolTipButton.isShowing){
            toolTipButton.dismiss()
        }
    }

    private fun showTooltipRecentCallButton() {
        try {
            if (this::toolTipRecentCall.isInitialized.not()) {
                toolTipRecentCall = Balloon.Builder(requireContext())
                    .setLayout(R.layout.layout_speaking_button_tooltip)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setIsVisibleArrow(true)
                    .setBackgroundColorResource(R.color.surface_tip)
                    .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                    .setWidthRatio(0.75f)
                    .setArrowPosition(0.5f)
                    .setDismissWhenTouchOutside(false)
                    .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                    .setLifecycleOwner(this)
                    .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                    .build()
                val textViewTitle = toolTipRecentCall.getContentView().findViewById<MaterialTextView>(R.id.title)
                textViewTitle.visibility = GONE
                val textViewSubHeadingText = toolTipRecentCall.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
                textViewSubHeadingText.text = "अच्छी बात नहीं हुई ? जानिये English में कैसे बात करें।"

                toolTipRecentCall.showAlignBottom(binding.imgRecentCallsHistory)
            }

        } catch (ex: Exception) {
            Log.d(TAG, "showBuyCourseTooltip: ${ex.message}")
        }
    }

    private fun dismissTooltipRecentButton(){
        if (this::toolTipRecentCall.isInitialized && toolTipRecentCall.isShowing){
            toolTipRecentCall.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::toolTipTopicContainer.isInitialized && toolTipTopicContainer.isShowing || this::toolTipButton.isInitialized && toolTipButton.isShowing) {
            viewModel.saveImpression(SPEAKING_BACK_PRESS)
        }

        dismissTooltipTopicContainer()
        dismissTooltipRecentButton()
        binding.welcomeContainer.visibility = GONE
        dismissTooltipButton()
        binding.btnPeerToPeerCall.visibility = VISIBLE
        Log.e("sagar", "onPause: ")
    }

}
