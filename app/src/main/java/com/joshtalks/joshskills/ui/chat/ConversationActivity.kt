package com.joshtalks.joshskills.ui.chat

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.Download
import com.google.android.material.button.MaterialButton
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.base.constants.IS_FIRST_CALL
import com.joshtalks.joshskills.constants.COURSE_RESTART_FAILURE
import com.joshtalks.joshskills.constants.COURSE_RESTART_SUCCESS
import com.joshtalks.joshskills.constants.INTERNET_FAILURE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_COURSE_BANNER_COUPON_UNLOCKED_TEXT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.COUPON_UNLOCK_LESSON_COUNT
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.LESSON_COMPLETE_COUPON_DISCOUNT
import com.joshtalks.joshskills.core.Utils.getCurrentMediaVolume
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.decorator.SmoothScrollingLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.extension.setImageWithPlaceholder
import com.joshtalks.joshskills.core.extension.setResourceImageDefault
import com.joshtalks.joshskills.core.extension.slideOutAnimation
import com.joshtalks.joshskills.core.interfaces.OnDismissWithSuccess
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.notification.HAS_COURSE_REPORT
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener.State.PAUSED
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.*
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.repository.server.chat_message.TAudioMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.chat.adapter.ConversationAdapter
import com.joshtalks.joshskills.ui.chat.extra.FirstCallBottomSheet
import com.joshtalks.joshskills.ui.chat.service.DownloadMediaService
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.fpp.SeeAllRequestsActivity
import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics.Event.MAIN_GROUP_ICON
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_UNLOCK_CLASS_ANIMATION
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.popup.PurchaseDialog
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.special_practice.SpecialPracticeActivity
import com.joshtalks.joshskills.ui.special_practice.utils.SPECIAL_ID
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.ui.tooltip.TooltipUtils
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.userprofile.models.Award
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import com.joshtalks.joshskills.ui.video_player.VIDEO_OBJECT
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.utils.getVoipState
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.UserInterestActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.StickyHeaderDecoration
import com.joshtalks.joshskills.voip.constant.State
import com.joshtalks.recordview.CustomImageButton.FIRST_STATE
import com.joshtalks.recordview.CustomImageButton.SECOND_STATE
import com.joshtalks.recordview.OnRecordListener
import com.joshtalks.recordview.OnRecordTouchListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

const val CHAT_ROOM_OBJECT = "chat_room"
const val UPDATED_CHAT_ROOM_OBJECT = "updated_chat_room"
const val CHAT_ROOM_ID = "chat_room_id"
const val IMAGE_SELECT_REQUEST_CODE = 1077
const val VISIBLE_ITEM_PERCENT = 75
const val PRACTISE_SUBMIT_REQUEST_CODE = 1100
const val COURSE_PROGRESS_REQUEST_CODE = 1101
const val VIDEO_OPEN_REQUEST_CODE = 1102
const val CONVERSATION_PRACTISE_REQUEST_CODE = 1105
const val ASSESSMENT_REQUEST_CODE = 1106
const val LESSON_REQUEST_CODE = 1107
const val CERTIFICATION_REQUEST_CODE = 1108
const val COURSE_PROGRESS_NEW_REQUEST_CODE = 1109
const val VOICE_CALL_REQUEST_CODE = 1110
const val DEFAULT_TOOLTIP_DELAY_IN_MS = 1000L
const val FREE_TRIAL_CALL_TOPIC_ID = "10"

const val FOCUS_ON_CHAT_ID = "focus_on_chat_id"

private const val TAG = "ConversationActivity"

class ConversationActivity :
    BaseConversationActivity(),
    Player.EventListener,
    ExoAudioPlayer.ProgressUpdateListener,
    AudioPlayerEventListener,
    OnDismissWithSuccess {

    companion object {
        fun startConversionActivity(activity: Activity, inboxEntity: InboxEntity) {
            val intent = Intent(activity, ConversationActivity::class.java).apply {
                putExtra(CHAT_ROOM_OBJECT, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            activity.startActivity(intent)
        }

        fun getConversationActivityIntent(activity: Activity, inboxEntity: InboxEntity) =
            Intent(activity, ConversationActivity::class.java).apply {
                putExtra(CHAT_ROOM_OBJECT, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
    }

    var openedLesson: Boolean = false
    var countdownTimerBack: CountDownTimer? = null
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var utilConversationViewModel: UtilConversationViewModel
    private lateinit var unlockClassViewModel: UnlockClassViewModel
    val event = EventLiveData
    private val conversationAdapter: ConversationAdapter by lazy {
        ConversationAdapter(WeakReference(this))
    }
    private var userProfileData: UserProfileResponse? = null
    private var currentAudioPosition: Int = -1
    private lateinit var conversationBinding: ActivityConversationBinding
    private lateinit var inboxEntity: InboxEntity
    private lateinit var activityRef: WeakReference<FragmentActivity>
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var revealAttachmentView: Boolean = false
    private val readChatList: MutableSet<ChatModel> = mutableSetOf()
    private var readMessageTimerTask: TimerTask? = null
    private var audioPlayerManager: ExoAudioPlayer? = null
    private var isOnlyChat = false
    private var flowFrom: String? = EMPTY
    private var loadingPreviousData = false
    private var isNewMessageShowing = false
    private var reachEndOfData = false
    private var refreshMessageByUser = false
    private var currentTooltipIndex = 0
    private var activityFeedControl = false
    private val leaderboardTooltipList by lazy {
        listOf(
            "English सीखने के लिए आप जितनी मेहनत करेंगे आपको उतने points मिलेंगे",
            "आपके सहपाठी कौन हैं और उनके कितने पॉइंट्स हैं आप यहाँ से देख सकते हैं"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.handler = this
        conversationBinding.executePendingBindings()
        activityRef = WeakReference(this)
        initIntentObject()
        if (::inboxEntity.isInitialized.not()) {
            this.finish()
            return
        }
        init()
    }

    override fun getConversationId(): String {
        return inboxEntity.conversation_id
    }

    private fun initIntentObject() {
        if (intent.hasExtra(CHAT_ROOM_OBJECT)) {
            flowFrom = "Inbox journey"
            val temp = intent.getParcelableExtra(CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this@ConversationActivity.finish()
                return
            }
            inboxEntity = temp
        }
        if (intent.hasExtra(UPDATED_CHAT_ROOM_OBJECT)) {
            flowFrom = "Notification"
            val temp = intent.getParcelableExtra(UPDATED_CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this@ConversationActivity.finish()
                return
            }
            inboxEntity = temp
        }
        if (intent.hasExtra(HAS_COURSE_REPORT)) {
            openCourseProgressListingScreen()
        }
        if (intent.hasExtra(FOCUS_ON_CHAT_ID)) {
            intent.getParcelableExtra<ChatModel>(FOCUS_ON_CHAT_ID)?.chatId?.run {
                scrollToPosition(this, animation = true)
            }
        }
        if (::inboxEntity.isInitialized) {
            conversationViewModel = ViewModelProvider(
                this, ConversationViewModelFactory(this, this.application, inboxEntity)
            ).get(ConversationViewModel::class.java)
            conversationBinding.viewmodel = conversationViewModel
            conversationBinding.lifecycleOwner = this

            utilConversationViewModel = ViewModelProvider(
                this, ConversationViewModelFactory(this, this.application, inboxEntity)
            ).get(UtilConversationViewModel::class.java)
            unlockClassViewModel = ViewModelProvider(
                this, ConversationViewModelFactory(this, this.application, inboxEntity)
            ).get(UnlockClassViewModel::class.java)
        }

        super.processIntent(intent)
    }

    private fun init() {
        initToolbar()
        initSharedPreferences()
        initABTest()
        initRV()
        initView()
        initFuture()
        addObservable()
        fetchMessage()
        readMessageDatabaseUpdate()
        showRestartButton()
    }

    private fun initSharedPreferences() {
        PrefManager.put(IS_FREE_TRIAL, inboxEntity.isCourseBought.not())
        if (inboxEntity.isCourseBought.not() || inboxEntity.courseId != DEFAULT_COURSE_ID)
            PrefManager.removeKey(IS_A2_C1_RETENTION_ENABLED)
        if (inboxEntity.isCourseBought && inboxEntity.isCapsuleCourse)
            PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
        if (inboxEntity.isCapsuleCourse)
            PrefManager.put(CHAT_OPENED_FOR_NOTIFICATION, true)
        if (inboxEntity.isCourseBought.not() &&
            inboxEntity.expiryDate != null &&
            inboxEntity.expiryDate!!.time < System.currentTimeMillis()
        ) {
            PrefManager.put(IS_FREE_TRIAL_ENDED, true)
        }
    }

    private fun initABTest() {
        conversationViewModel.getCampaignData(CampaignKeys.ACTIVITY_FEED_V2.name)
        conversationViewModel.getCampaignData(CampaignKeys.A2_C1.name)
    }

    private fun getAllPendingRequest() {
        conversationViewModel.getPendingRequestsList()
    }

    private fun initFreeTrialTimer() {
        PrefManager.put(IS_COURSE_BOUGHT, inboxEntity.isCourseBought)
        PrefManager.put(IS_FREE_TRIAL, inboxEntity.isCourseBought.not())
        if (inboxEntity.isCourseBought) {
            conversationBinding.freeTrialExpiryLayout.visibility = GONE
            return
        } else if (
            inboxEntity.expiryDate != null &&
            inboxEntity.expiryDate!!.time >= System.currentTimeMillis()
        ) {
            if (inboxEntity.expiryDate!!.time > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                conversationBinding.freeTrialExpiryLayout.visibility = GONE
                conversationBinding.freeTrialContainer.gone()
            } else {
                conversationBinding.freeTrialContainer.visibility = VISIBLE
                if (inboxEntity.expiryDate!!.time > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                    conversationBinding.freeTrialExpiryLayout.visibility = GONE
                    conversationBinding.freeTrialContainer.gone()
                } else {
                    conversationBinding.freeTrialContainer.visibility = VISIBLE
                    startTimer(inboxEntity.expiryDate!!.time - System.currentTimeMillis())
                }
            }
        } else if (
            inboxEntity.expiryDate != null &&
            inboxEntity.expiryDate!!.time < System.currentTimeMillis()
        ) {
            PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            PrefManager.put(COURSE_EXPIRY_TIME_IN_MS, inboxEntity.expiryDate!!.time)
            conversationBinding.freeTrialContainer.visibility = VISIBLE
//            conversationBinding.trialTimerView.endFreeTrial()
            conversationBinding.freeTrialText.text = getString(R.string.free_trial_ended)
            conversationBinding.freeTrialExpiryLayout.visibility = VISIBLE
        }
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        if (countdownTimerBack != null)
            countdownTimerBack?.cancel()
        countdownTimerBack = object : CountDownTimer(startTimeInMilliSeconds, 1000) {
            override fun onTick(millis: Long) {
//                AppObjectController.uiHandler.post {
//                    conversationBinding.trialTimerView.startTimer(millis)
//                }
                conversationBinding.freeTrialText.text = getString(
                    R.string.free_trial_end_in,
                    UtilTime.timeFormatted(millis)
                )
            }

            override fun onFinish() {
                AppObjectController.uiHandler.post {
                    countdownTimerBack?.cancel()
//                    conversationBinding.trialTimerView.endFreeTrial()
                    conversationBinding.freeTrialText.text = getString(R.string.free_trial_ended)

                }
            }
        }
        countdownTimerBack?.start()
    }

    fun hideLeaderboardTooltip() {
        conversationBinding.leaderboardTooltipLayout.visibility = GONE
        PrefManager.put(HAS_SEEN_LEADERBOARD_TOOLTIP, true)
    }

    private fun showLeaderBoardSpotlight() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1500)
            window.statusBarColor = ContextCompat.getColor(
                this@ConversationActivity,
                R.color.primary_800
            )
            conversationBinding.freeTrialContainer.visibility = GONE
            conversationBinding.overlayLayout.visibility = VISIBLE
            conversationBinding.arrowAnimation.visibility = VISIBLE
            conversationBinding.overlayLeaderboardContainer.visibility = VISIBLE
            slideInAnimation(conversationBinding.overlayLeaderboardTooltip)
            conversationBinding.labelTapToDismiss.visibility = GONE
            PrefManager.put(HAS_SEEN_LEADERBOARD_ANIMATION, true)
            delay(600)
            conversationBinding.overlayLayout.setOnClickListener {
                PrefManager.put(HAS_SEEN_LEADERBOARD_ANIMATION, true)
                hideLeaderBoardSpotlight()
            }
            conversationBinding.labelTapToDismiss.visibility = VISIBLE
            conversationBinding.labelTapToDismiss.startAnimation(
                AnimationUtils.loadAnimation(this@ConversationActivity, R.anim.slide_up_dialog)
            )
        }
    }

    private fun hideLeaderBoardSpotlight() {
        initFreeTrialTimer()
        conversationBinding.overlayLayout.setOnClickListener(null)
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_color)
        conversationBinding.overlayLayout.visibility = GONE
        conversationBinding.arrowAnimation.visibility = GONE
        conversationBinding.overlayLeaderboardContainer.visibility = GONE
        conversationBinding.labelTapToDismiss.visibility = GONE
        conversationBinding.overlayLeaderboardTooltip.visibility = GONE
    }

    private fun showFirstCallBottomSheet() {
        if (getVoipState() == State.IDLE &&
            PrefManager.getIntValue(FT_CALLS_LEFT) == 15 &&
            PrefManager.getBoolValue(IS_COURSE_BOUGHT).not()
        )
            lifecycleScope.launch(Dispatchers.Main) {
                delay(300)
                FirstCallBottomSheet.showDialog(supportFragmentManager)
            }
    }

    fun showFreeTrialPaymentScreen() {
        MixPanelTracker.publishEvent(MixPanelEvent.FREE_TRIAL_ENDED_BUY_NOW).push()
        BuyPageActivity.startBuyPageActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            "CONVERSATION_FT_ENDED_BTN"
        )
    }

    private fun initToolbar() {
        var obj = JSONObject()
        if (inboxEntity.isCourseBought)
            obj.put("is paid", true)
        else
            obj.put("is paid", false)

        try {
            conversationBinding.textMessageTitle.text = inboxEntity.course_name
            conversationBinding.imageViewLogo.setImageWithPlaceholder(inboxEntity.course_icon)
            conversationBinding.imageViewLogo.visibility = VISIBLE
            if (inboxEntity.isCapsuleCourse) {
                conversationBinding.imageViewLogo.setOnClickListener {
                    openCourseProgressListingScreen()
                }
                conversationBinding.textMessageTitle.setOnClickListener {
                    MixPanelTracker.publishEvent(MixPanelEvent.COURSE_OVERVIEW).push()
                    openCourseProgressListingScreen()
                }
            }

            conversationBinding.ivBack.setOnClickListener {
                val resultIntent = Intent()
                setResult(RESULT_OK, resultIntent)
                finish()
                MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
            }

            if (inboxEntity.isCourseBought) {
                conversationBinding.ivIconReferral.visible()
//                conversationBinding.btnUpgrade.gone()
            } else {
//                inboxEntity.isCapsuleCourse
//                if (inboxEntity.courseId == DEFAULT_COURSE_ID)
////                    conversationBinding.btnUpgrade.visible()
                conversationBinding.ivIconReferral.gone()
            }
            conversationBinding.ivIconReferral.setOnClickListener {
                conversationViewModel.saveImpression(IMPRESSION_REFER_VIA_CONVERSATION_ICON)

                ReferralActivity.startReferralActivity(
                    this@ConversationActivity,
                    ConversationActivity::class.java.name
                )
                MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
            }

            conversationBinding.toolbar.inflateMenu(R.menu.conversation_menu)
            profileFeatureActiveView(inboxEntity.isCapsuleCourse)
            showFavtMenuOption(inboxEntity.isCapsuleCourse)
            showRestartMenuOption()
            showInterestMenuOption(inboxEntity.isCapsuleCourse)
            conversationBinding.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {
                        conversationViewModel.saveImpression(IMPRESSION_REFER_VIA_CONVERSATION_MENU)
                        MixPanelTracker.publishEvent(MixPanelEvent.REFERRAL_OPENED).push()
                        ReferralActivity.startReferralActivity(
                            this@ConversationActivity,
                            ConversationActivity::class.java.name
                        )
                    }
                    R.id.menu_clear_media -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.CLEAR_ALL_MEDIA_CLICKED).push()
                        clearMediaFromInternal(inboxEntity.conversation_id)
                    }
                    R.id.menu_help -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.HELP).push()
                        openHelpActivity()
                    }
                    R.id.profile_setting -> {
                        openUserProfileActivity(
                            Mentor.getInstance().getId(),
                            USER_PROFILE_FLOW_FROM.MENU.value
                        )
                    }
                    R.id.leaderboard_setting -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.LEADERBOARD_OPENED).push()
                        openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
                    }
                    R.id.menu_favorite_list -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.FAVORITE_LIST).push()
                        FavoriteListActivity.openFavoriteCallerActivity(
                            this,
                            inboxEntity.conversation_id
                        )
                    }
                    R.id.menu_interest -> {
                        val intent = Intent(this, UserInterestActivity::class.java)
                        intent.putExtra("isEditCall", true)
                        startActivity(intent)
                        conversationViewModel.saveImpression(INTEREST_FORM_OPEN_MENU_EDIT)
                    }
                    R.id.menu_restart_course -> {
                        MixPanelTracker.publishEvent(MixPanelEvent.RESTART_COURSE_CLICKED)
                            .addParam(ParamKeys.CLICKED_FROM, "3 dot")
                            .push()
                        conversationViewModel.saveRestartCourseImpression(
                            IMPRESSION_CLICK_RESTART_COURSE_DOT
                        )
                        restartCourse(false)
                    }
                }
                return@setOnMenuItemClickListener true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun restartCourse(isFromRestartButton: Boolean) {
        if (isFromRestartButton) {
            conversationViewModel.saveRestartCourseImpression(IMPRESSION_CLICK_RESTART_90LESSONS)
            MixPanelTracker.publishEvent(MixPanelEvent.RESTART_COURSE_CLICKED)
                .addParam(ParamKeys.CLICKED_FROM, "button")
                .push()
        }

        var phoneNumber = User.getInstance().phoneNumber
        if (!phoneNumber.isNullOrEmpty()) {
            if (phoneNumber[0] == '+' && phoneNumber[1] == '9' && phoneNumber[2] == '1')
                phoneNumber = phoneNumber?.substring(3)
            else if (phoneNumber[0] == '+' && phoneNumber[1] == '8' && phoneNumber[2] == '8' && phoneNumber[3] == '0')
                phoneNumber = phoneNumber?.substring(4)
        }
        val email = User.getInstance().email
        if (email.isNullOrEmpty() && phoneNumber.isNullOrEmpty()) {
            showToast(getString(R.string.course_restart_fail))
        } else {
            MaterialDialog(this@ConversationActivity).show {
                message(R.string.restart_course_message)
                positiveButton(R.string.restart_now) {
                    if (!phoneNumber.isNullOrEmpty()) {
                        conversationBinding.btnRestartCourse.visibility = View.GONE
                        conversationViewModel.restartCourse(phoneNumber.toString(), "MobileNumber")
                    } else if (phoneNumber.isNullOrEmpty() && !email.isNullOrEmpty()) {
                        conversationBinding.btnRestartCourse.visibility = View.GONE
                        conversationViewModel.restartCourse(email, "Email")
                    }
                    if (isFromRestartButton) {
                        conversationViewModel.saveRestartCourseImpression(
                            IMPRESSION_SUCCESS_RESTART_90LESSONS
                        )
                    } else {
                        conversationViewModel.saveRestartCourseImpression(
                            IMPRESSION_SUCCESS_RESTART_COURSE_DOT
                        )
                    }
                    MixPanelTracker.publishEvent(MixPanelEvent.RESTART_COURSE_NOW).push()
                }
                negativeButton(R.string.cancel) {
                    MixPanelTracker.publishEvent(MixPanelEvent.RESTART_COURSE_CANCEL).push()
                }
            }
        }
    }

    private fun showRestartButton() {
        CoroutineScope(Dispatchers.IO).launch {
            val lastLesson = conversationViewModel.getLastLessonForCourse()
            if (lastLesson == 90 && inboxEntity.isCapsuleCourse && inboxEntity.isCourseBought) {
                withContext(Dispatchers.Main) {
                    conversationBinding.btnRestartCourse.visibility = VISIBLE
                    conversationBinding.messageButton.visibility = GONE
                    conversationBinding.chatEdit.visibility = GONE
                }
            }
        }
    }

    private fun initRV() {
        linearLayoutManager = SmoothScrollingLinearLayoutManager(this, false)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.isItemPrefetchEnabled = true
        linearLayoutManager.initialPrefetchItemCount = 20
        linearLayoutManager.isSmoothScrollbarEnabled = true
        conversationBinding.chatRv.apply {
            addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(context, 4f)))
        }
        conversationBinding.chatRv.layoutManager = linearLayoutManager
        conversationBinding.chatRv.itemAnimator = null
        conversationBinding.chatRv.setHasFixedSize(false)

        conversationBinding.chatRv.addItemDecoration(StickyHeaderDecoration(conversationAdapter), 0)
        conversationAdapter.initializePool(conversationBinding.chatRv.recycledViewPool)
        conversationBinding.chatRv.adapter = conversationAdapter
        conversationBinding.chatRv.layoutManager?.isMeasurementCacheEnabled = false

        conversationBinding.chatRv.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = GONE
                } else if (recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    conversationBinding.scrollToEndButton.visibility = VISIBLE
                }
                visibleItem()
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (recyclerView.canScrollVertically(-1)) {
                    getPreviousRecord()
                }
            }
        })
    }

    private fun getLayoutManager(): SmoothScrollingLinearLayoutManager {
        return linearLayoutManager as SmoothScrollingLinearLayoutManager
    }

    private fun getPreviousRecord() {
        if (loadingPreviousData.not() && linearLayoutManager.findFirstVisibleItemPosition() in 0..8) {
            loadingPreviousData = true
            conversationViewModel.loadPagingMessage(conversationAdapter.getFirstItem())
        }
    }

    private fun initView() {
        conversationBinding.scrollToEndButton.setOnClickListener {
            scrollToEnd()
        }

        if (inboxEntity.isCourseBought && inboxEntity.isCapsuleCourse) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
        }

        conversationBinding.imgFppBtn.setOnSingleClickListener {
            if (inboxEntity.isCourseBought.not() &&
                inboxEntity.expiryDate != null &&
                inboxEntity.expiryDate!!.time < System.currentTimeMillis()
            ) {
                val nameArr = User.getInstance().firstName?.split(" ")
                val firstName = if (nameArr != null) nameArr[0] else EMPTY
                showToast(getString(R.string.feature_locked, firstName))
            } else {
                MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUESTS_ICON_CLICKED).push()
                val intent = Intent(this, SeeAllRequestsActivity::class.java)
                startActivity(intent)
            }
        }

        conversationBinding.imgGroupChatBtn.setOnSingleClickListener {
            if (inboxEntity.isCourseBought.not() &&
                inboxEntity.expiryDate != null &&
                inboxEntity.expiryDate!!.time < System.currentTimeMillis()
            ) {
                val nameArr = User.getInstance().firstName?.split(" ")
                val firstName = if (nameArr != null) nameArr[0] else EMPTY
                showToast(getString(R.string.feature_locked, firstName))
            } else {
                MixPanelTracker.publishEvent(MixPanelEvent.GROUP_ICON_CLICKED).push()
                val intent = Intent(this, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, getConversationId())
                }
                GroupAnalytics.push(MAIN_GROUP_ICON)
                startActivity(intent)
            }
        }

        conversationBinding.leaderboardBtnClose.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.LEADERBOARD_CANCEL).push()
            conversationBinding.userPointContainer.slideOutAnimation()
            // hideLeaderboardTooltip()
        }

        conversationBinding.leaderboardTxt.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.LEADERBOARD_OPENED).push()
            openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
        }
        conversationBinding.overlayLeaderboardContainer.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.LEADERBOARD_OPENED).push()
            PrefManager.put(HAS_SEEN_LEADERBOARD_ANIMATION, true)
            openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
            hideLeaderBoardSpotlight()
        }
        conversationBinding.points.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.CONVERSATION_POINTS_CLICKED).push()
            openUserProfileActivity(
                Mentor.getInstance().getId(),
                USER_PROFILE_FLOW_FROM.FLOATING_BAR.value
            )
        }

        conversationBinding.refreshLayout.setOnRefreshListener {
            if (internetAvailableFlag) {
                conversationBinding.refreshLayout.isRefreshing = true
                conversationViewModel.refreshChatOnManual()
            } else {
                conversationBinding.refreshLayout.isRefreshing = false
            }
        }

        findViewById<View>(R.id.ll_audio).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.AUDIO_SELECTED.NAME).push()
            addAttachmentUIUpdate()
            PermissionUtils.storageReadAndWritePermission(
                activityRef.get()!!,
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                bottomAudioAttachment()
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                    activityRef.get()!!
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

        findViewById<View>(R.id.ll_camera).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.CAMERA_SELECTED.NAME).push()
            addAttachmentUIUpdate()
            uploadImageByUser()
        }
        conversationBinding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
    }

    fun moveToPaymentActivity(v: View) {
        MixPanelTracker.publishEvent(MixPanelEvent.FREE_TRIAL_ENDED_BUY_NOW).push()
        BuyPageActivity.startBuyPageActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            "CONVERSATION_FT_TIMER"
        )
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < leaderboardTooltipList.size - 1) {
            currentTooltipIndex++
            conversationBinding.joshTextView.text = leaderboardTooltipList[currentTooltipIndex]
            conversationBinding.txtTooltipIndex.text = "${currentTooltipIndex + 1} of ${leaderboardTooltipList.size}"
        } else {
            conversationBinding.leaderboardTooltipLayout.visibility = GONE
            PrefManager.put(HAS_SEEN_LEADERBOARD_TOOLTIP, true)
        }
    }

    private fun initFuture() {
        AppAnalytics.create(AnalyticsEvent.COURSE_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
            .push()
        audioPlayerManager = ExoAudioPlayer.getInstance()
        audioPlayerManager?.setProgressUpdateListener(this@ConversationActivity)
        audioPlayerManager?.playerListener = this@ConversationActivity
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT))
            onlyChatView()
        else {
            conversationBinding.bottomBar.visibility = GONE
        }
        initSnackBar()
    }

    private fun onlyChatView() {
        inboxEntity.chat_type?.let {
            when {
                it.equals("NC", ignoreCase = true) -> {
                    isOnlyChat = true
                    conversationBinding.flAttachment.visibility = GONE
                    conversationBinding.quickToggle.visibility = GONE
                    conversationBinding.recordButton.visibility = INVISIBLE
                    conversationBinding.attachmentContainer.visibility = GONE
                    conversationBinding.messageButton.visibility = VISIBLE
                    conversationBinding.messageButton.setImageResource(R.drawable.ic_send)
                    conversationBinding.messageButton.setResourceImageDefault(R.drawable.ic_send)
                    initInputUI()
                }
                it.equals("RC", ignoreCase = true) -> {
                    conversationBinding.bottomBar.visibility = GONE
                }
                else -> {
                    initRecordUI()
                    initInputUI()
                }
            }
        }
    }

    private fun initRecordUI() {
        conversationBinding.recordButton.setRecordView(conversationBinding.recordView)
        conversationBinding.recordView.cancelBounds = 2f
        conversationBinding.recordView.setSmallMicColor(Color.parseColor("#c2185b"))
        conversationBinding.recordView.setLessThanSecondAllowed(false)
        conversationBinding.recordView.setSlideToCancelText(getString(R.string.slide_to_cancel))
        conversationBinding.recordView.setCustomSounds(R.raw.record_start, R.raw.record_finished, 0)
        conversationBinding.recordButton.isListenForRecord =
            PermissionUtils.checkPermissionForAudioRecord(this@ConversationActivity)
        conversationBinding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
                if (isCallOngoing()) {
                    return
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                AppAnalytics.create(AnalyticsEvent.AUDIO_BUTTON_CLICKED.NAME).push()
                conversationBinding.recordView.visibility = VISIBLE
                conversationViewModel.startRecord(null)
                AppAnalytics.create(AnalyticsEvent.AUDIO_RECORD.NAME).push()
            }

            override fun onCancel() {
                conversationViewModel.stopRecording(true)
            }

            override fun onFinish(recordTime: Long) {
                try {
                    AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                    conversationBinding.recordView.visibility = GONE
                    conversationViewModel.stopRecording(false)
                    conversationViewModel.recordFile.let {
                        addRecordedAudioMessage(it.absolutePath)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun onLessThanSecond() {
                conversationBinding.recordView.visibility = GONE
                conversationViewModel.stopRecording(true)
                AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            }
        })

        conversationBinding.recordView.setOnBasketAnimationEndListener {
            conversationBinding.recordView.visibility = GONE
            conversationViewModel.stopRecording(true)
            AppAnalytics.create(AnalyticsEvent.AUDIO_CANCELLED.NAME).push()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        conversationBinding.recordButton.setOnTouchListener(
            OnRecordTouchListener {
                if (isCallOngoing()) {
                    return@OnRecordTouchListener
                }
                if (conversationBinding.chatEdit.text.toString()
                        .isEmpty() && it == MotionEvent.ACTION_DOWN
                ) {
                    if (PermissionUtils.isAudioAndStoragePermissionEnable(this).not()) {
                        PermissionUtils.audioRecordStorageReadAndWritePermission(
                            activityRef.get()!!,
                            object : MultiplePermissionsListener {
                                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                    report?.areAllPermissionsGranted()?.let { flag ->
                                        if (flag) {
                                            conversationBinding.recordButton.isListenForRecord =
                                                true
                                            return@let
                                        }
                                        if (report.isAnyPermissionPermanentlyDenied) {
                                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                                this@ConversationActivity,
                                                R.string.record_permission_message
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
                    } else {
                        conversationBinding.recordButton.isListenForRecord = true
                    }
                }
            }
        )

        conversationBinding.recordButton.setOnRecordClickListener {
            sendTextMessage()
        }
    }

    private fun initInputUI() {
        conversationBinding.chatEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    conversationBinding.recordButton.goToState(FIRST_STATE)
                    conversationBinding.recordButton.isListenForRecord =
                        PermissionUtils.checkPermissionForAudioRecord(this@ConversationActivity)
                    if (isOnlyChat.not()) {
                        conversationBinding.quickToggle.show()
                    }
                } else {
                    conversationBinding.recordButton.goToState(SECOND_STATE)
                    conversationBinding.recordButton.isListenForRecord = false
                    conversationBinding.quickToggle.hide()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        conversationBinding.messageButton.setOnClickListener {
            sendTextMessage()
        }
    }

    private fun openUserProfileActivity(id: String, previousPage: String?) {
        previousPage?.let {
            UserProfileActivity.startUserProfileActivity(
                this,
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                null,
                it,
                conversationId = inboxEntity.conversation_id,
            )
        }
    }

    private fun fetchMessage() {
        conversationViewModel.getAllCourseMessage()
    }

    private fun addObservable() {
        conversationViewModel.pendingRequestsList.observe(this) {
            with(conversationBinding) {
                if (it.pendingRequestsList.isNotEmpty() && inboxEntity.isCourseBought && inboxEntity.isCapsuleCourse) {
                    fppRequestCountNumber.text = it.pendingRequestsList.size.toString()
                    fppRequestCountNumber.visibility = VISIBLE
                } else {
                    fppRequestCountNumber.visibility = GONE
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            utilConversationViewModel.userData.collectLatest { userProfileData ->
                this@ConversationActivity.userProfileData = userProfileData
                conversationBinding.imgFppBtn.isVisible =
                    PrefManager.getBoolValue(IS_COURSE_BOUGHT) && inboxEntity.isCapsuleCourse
                if (userProfileData.hasGroupAccess && PrefManager.getStringValue(CURRENT_COURSE_ID) == DEFAULT_COURSE_ID) {
                    conversationBinding.imgGroupChatBtn.visibility = VISIBLE
                    if (!PrefManager.getBoolValue(ONE_GROUP_REQUEST_SENT)) {
                        PrefManager.put(ONE_GROUP_REQUEST_SENT, conversationViewModel.getClosedGroupCount() != 0)
                        conversationBinding.ringingIcon.visibility = VISIBLE
                    }
                } else {
                    conversationBinding.imgGroupChatBtn.visibility = GONE
                }
                getAllPendingRequest()
                initScoreCardView(userProfileData)
                if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE))
                    profileFeatureActiveView(true)
            }
        }

        lifecycleScope.launchWhenCreated {
            conversationViewModel.userUnreadCourseChat.collectLatest { items ->
                //  Start Add new Message UI add logic
                if (items.isEmpty()) {
                    return@collectLatest
                }
                addRVPatch(items.size + conversationAdapter.itemCount - 1)
                if (isNewMessageShowing.not()) {
                    val index = items.indexOfFirst { it.isSeen.not() }
                    val haveUserMessage = items.any { it.sender != null }
                    if (index > -1 && haveUserMessage.not()) {
                        conversationAdapter.addMessagesList(arrayListOf(getNewMessageObj(items.first().created)))
                        getLayoutManager().smoothScrollToPosition(
                            this@ConversationActivity,
                            conversationAdapter.itemCount + index,
                            25F
                        )
                        isNewMessageShowing = true
                    } else {
                        scrollToEnd()
                    }
                }
                // End Logic
                conversationAdapter.addMessagesList(items)
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.userReadCourseChat.collectLatest { items ->
                if (items.isNotEmpty()) {
                    addRVPatch(items.size + conversationAdapter.itemCount)
                    conversationAdapter.addMessagesList(items)
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.pagingMessagesChat.collectLatest { items ->
                loadingPreviousData = false
                addRVPatch(items.size + conversationAdapter.itemCount)
                reachEndOfData = conversationAdapter.addMessageAboveMessage(items)
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.updateChatMessage.collectLatest { chat ->
                chat?.let {
                    conversationAdapter.updateItem(it)
                    unlockClassViewModel.canWeAddUnlockNextClass(it.chatId)
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            conversationViewModel.newMessageAddFlow.collectLatest {
                if (refreshMessageByUser) {
                    getLayoutManager().smoothScrollToPosition(
                        this@ConversationActivity,
                        conversationAdapter.itemCount + 1,
                        25F
                    )
                }
                refreshMessageByUser = false
            }
        }
        lifecycleScope.launchWhenCreated {
            unlockClassViewModel.unlockNextClass.collectLatest { flag ->
                hideProgressBar()
                if (flag) {
                    val message = getUnlockClassMessage(conversationAdapter.getLastItem())
                    val isAdded = conversationAdapter.addUnlockClassMessage(message)
                    val cPosition = linearLayoutManager.findLastVisibleItemPosition()
                    val lastVisiblePosition =
                        linearLayoutManager.findLastCompletelyVisibleItemPosition()
                    if (isAdded && (cPosition >= conversationAdapter.itemCount - 1 || lastVisiblePosition >= conversationAdapter.itemCount - 1)) {
                        getLayoutManager().smoothScrollToPosition(
                            this@ConversationActivity,
                            conversationAdapter.itemCount,
                            25F
                        )
                    }
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            unlockClassViewModel.batchChange.collectLatest {
                when (it) {
                    BATCH_CHANGE_SUCCESS -> {
                        conversationAdapter.removeUnlockMessage()
                        conversationViewModel.refreshChatOnManual()
                    }
                    BATCH_CHANGE_FAILURE -> {
                        conversationAdapter.removeUnlockMessage()
                        conversationBinding.refreshLayout.isRefreshing = false
                    }
                    BATCH_CHANGE_BUY_FIRST -> {
                        conversationBinding.refreshLayout.isRefreshing = false
                        conversationViewModel.getCoursePopupData(PurchasePopupType.LESSON_LOCKED)
                    }
                }
            }
        }
        conversationViewModel.coursePopupData.observe(this) {
            it?.let {
                PurchaseDialog.newInstance(it).show(supportFragmentManager, "PurchaseDialog")
            }
        }
        utilConversationViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        }

        conversationViewModel.abTestCampaignliveData.observe(this) { abTestCampaignData ->
            abTestCampaignData?.let { map ->
                if (abTestCampaignData.campaignKey == CampaignKeys.ACTIVITY_FEED_V2.name) {
                    activityFeedControl =
                        (map.variantKey == VariantKeys.AF2_ENABLED.name) && map.variableMap?.isEnabled == true
                } else if (abTestCampaignData.campaignKey == CampaignKeys.A2_C1.name) {
                    if (inboxEntity.isCourseBought && inboxEntity.courseId == DEFAULT_COURSE_ID && abTestCampaignData.isCampaignActive) {
                        PrefManager.put(
                            IS_A2_C1_RETENTION_ENABLED,
                            (map.variantKey == VariantKeys.A2_C1_RETENTION.name) && map.variableMap?.isEnabled == true
                        )
                    } else {
                        PrefManager.removeKey(IS_A2_C1_RETENTION_ENABLED)
                    }
                }
            }
        }
    }

    private fun addRVPatch(count: Int) {
        if (count <= 3) {
            linearLayoutManager.stackFromEnd = false
        }
    }

    private fun bottomAudioAttachment() {
        addAttachmentUIUpdate()
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        MediaPicker.with(this, MediaPicker.MediaTypes.AUDIO)
            .setConfig(pickerConfig)
            .onResult()
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    it?.getOrNull(0)?.path?.let { path ->
                        if (path.isNotBlank()) {
                            AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                            addAudioFromBottomBar(Utils.getPathFromUri(path))
                        }
                    }
                },
                {
                }
            )
    }

    override fun onRestart() {
        super.onRestart()
        getAllPendingRequest()
    }

    private fun profileFeatureActiveView(showLeaderboardMenu: Boolean) {
        if (showLeaderboardMenu) {
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isEnabled = true
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.leaderboard_setting).isEnabled = false
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.profile_setting).isEnabled = false
        }
    }

    private fun showFavtMenuOption(showMenu: Boolean) {
        if (showMenu && inboxEntity.isCourseBought) {
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isEnabled = false
        }
    }

    private fun showRestartMenuOption() {
        if (inboxEntity.isCourseBought && inboxEntity.isCapsuleCourse) {
            conversationBinding.toolbar.menu.findItem(R.id.menu_restart_course).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.menu_restart_course).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.menu_restart_course).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.menu_restart_course).isEnabled = false
        }
    }

    private fun showInterestMenuOption(showMenu: Boolean) {
        if (showMenu) {
            conversationBinding.toolbar.menu.findItem(R.id.menu_interest).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.menu_interest).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.menu_interest).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.menu_interest).isEnabled = false
        }
    }

    private fun initScoreCardView(userData: UserProfileResponse) {
        userData.isContainerVisible?.let { isLeaderBoardActive ->
            if (isLeaderBoardActive) {
                conversationBinding.points.text = userData.points.toString().plus(" Points")
                conversationBinding.userPointContainer.visibility = VISIBLE
                if (PrefManager.getBoolValue(HAS_SEEN_UNLOCK_CLASS_ANIMATION) &&
                    !PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ANIMATION) &&
                    PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED, defValue = false).not()
                ) {
                    showLeaderBoardSpotlight()
                } else if (!PrefManager.getBoolValue(HAS_SEEN_UNLOCK_CLASS_ANIMATION)) {
                    CoroutineScope(Dispatchers.Main).launch {
                        setOverlayAnimation()
                    }
                }
            } else {
                conversationBinding.userPointContainer.visibility = GONE
            }
        }
        val unseenAwards: ArrayList<Award> = ArrayList()
        userData.awardCategory?.parallelStream()?.forEach { ac ->
            ac.awards?.filter { it.isSeen == false && it.is_achieved }
                ?.let { unseenAwards.addAll(it) }
        }
        if (unseenAwards.isNotEmpty()) {
//            showAward(unseenAwards)
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(DBInsertion::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (conversationAdapter.itemCount == 0) {
                            conversationViewModel.addNewMessages(0.0)
                        } else {
                            conversationAdapter.getLastItemV2()?.messageTime?.let {
                                conversationViewModel.addNewMessages(it)
                            }
                        }
                        refreshMessageByUser = it.refreshMessageUser
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(PlayVideoEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        conversationViewModel.setMRefreshControl(false)
                        VideoPlayerActivity.startConversionActivity(
                            this,
                            it.chatModel,
                            inboxEntity.course_name,
                            inboxEntity.duration,
                            conversationId = inboxEntity.conversation_id
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Utils.fileUrl(it.localPath, it.serverPath)?.run {
                            ImageShowFragment.newInstance(
                                this,
                                inboxEntity.course_name,
                                it.imageId
                            )
                                .show(supportFragmentManager, "ImageShow")
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(PdfOpenEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        PdfViewerActivity.startPdfActivity(
                            activityRef.get()!!,
                            it.pdfObject.id,
                            inboxEntity.course_name,
                            it.chatId,
                            conversationId = inboxEntity.conversation_id
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (it.state == Download.STATE_COMPLETED) {
                            refreshViewAtPos(
                                AppObjectController.gsonMapperForLocal.fromJson(
                                    it.id,
                                    ChatModel::class.java
                                )
                            )
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listen(DownloadMediaEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it.downloadStatus) {
                        DOWNLOAD_STATUS.DOWNLOADED -> {
                            conversationViewModel.refreshMessageObject(it.id)
                        }
                        DOWNLOAD_STATUS.DOWNLOADING -> {
                            DownloadMediaService.addDownload(it.chatModel, it.url)
                        }
                        DOWNLOAD_STATUS.REQUEST_DOWNLOADING -> {
                            PermissionUtils.storageReadAndWritePermission(
                                this,
                                object : MultiplePermissionsListener {
                                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                                        report?.areAllPermissionsGranted()?.let { flag ->
                                            if (flag && internetAvailableFlag.not()) {
                                                showToast(getString(R.string.internet_not_available_msz))
                                                return@let
                                            }
                                            val chatModel = it.chatModel
                                            chatModel?.downloadStatus =
                                                DOWNLOAD_STATUS.DOWNLOADING
                                            chatModel?.let {
                                                conversationAdapter.updateItem(it)
                                            }
                                            if (it.type == BASE_MESSAGE_TYPE.PD || it.type == BASE_MESSAGE_TYPE.AU) {
                                                DownloadMediaService.addDownload(
                                                    it.chatModel,
                                                    it.url
                                                )
                                            } else if (it.type == BASE_MESSAGE_TYPE.VI) {
                                                if (AppObjectController.getVideoTracker() != null) {
                                                    AppObjectController.videoDownloadTracker.download(
                                                        it.chatModel,
                                                        Uri.parse(it.url),
                                                        VideoDownloadController.getInstance()
                                                            .buildRenderersFactory(true)
                                                    )
                                                }
                                            }
                                            return
                                        }
                                        report?.isAnyPermissionPermanentlyDenied?.let {
                                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                                activityRef.get()!!
                                            )
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
                        else -> {}
                    }
                }
        )

        compositeDisposable.add(
            RxBus2.listen(DownloadCompletedEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val obj = AppObjectController.appDatabase.chatDao()
                            .getUpdatedChatObjectViaId(it.chatModel.chatId)
                        refreshViewAtPos(obj)
                    }
                }
        )
        compositeDisposable.add(
            RxBus2.listen(VideoDownloadedBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val chatObj = AppObjectController.appDatabase.chatDao()
                            .getUpdatedChatObjectViaId(it.messageObject.chatId)
                        refreshViewAtPos(chatObj)
                    }
                }
        )
        compositeDisposable.add(
            RxBus2.listen(ChatModel::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    visibleItem()
                }
        )

        //  Start Block for swipe to refresh and get chat
        compositeDisposable.add(
            RxBus2.listen(MessageCompleteEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (conversationBinding.refreshLayout.isRefreshing) {
                        val message: String = if (it.flag) {
                            //getString(R.string.new_message_arrive)
                            EMPTY
                        } else {
                            getString(R.string.no_new_message_arrive)
                        }
                        if (message.isBlank().not()) {
                            StyleableToast.Builder(this).gravity(Gravity.BOTTOM)
                                .text(message).cornerRadius(16).length(Toast.LENGTH_LONG)
                                .solidBackground().show()
                        }
                    }
                    if (it.flag.not()) {
                        hideProgressBar()
                    }
                    conversationBinding.refreshLayout.isRefreshing = false
                }
        )
        // End

        compositeDisposable.add(
            RxBus2.listen(AudioPlayEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (getCurrentMediaVolume(applicationContext) <= 0) {
                            StyleableToast.Builder(applicationContext).gravity(Gravity.BOTTOM)
                                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                                .length(Toast.LENGTH_LONG)
                                .solidBackground().show()
                        }
                        if (it.state == PAUSED) {
                            audioPlayerManager?.onPause()
                            return@subscribe
                        }
                        AppObjectController.currentPlayingAudioObject?.let { chatModel ->
                            refreshViewAtPos(chatModel)
                        }
                        analyticsAudioPlayed(it.audioType)
                        currentAudioPosition =
                            conversationAdapter.getMessagePositionById(it.chatModel.chatId)
                        if (AppObjectController.currentPlayingAudioObject != null && ExoAudioPlayer.LAST_ID == it?.chatModel?.chatId) {
                            audioPlayerManager?.resumeOrPause()
                        } else {
                            AppObjectController.currentPlayingAudioObject = it.chatModel
                            audioPlayerManager?.onPause()
                            setPlayProgress(it.chatModel.playProgress)
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            audioPlayerManager?.play(
                                it.audioType!!.audio_url,
                                it.chatModel.chatId
                            )
                            conversationViewModel.saveAudioImpression(VOICE_NOTE_PLAYED, it.chatModel.chatId)
                        }
                        DatabaseUtils.updateLastUsedModification(it.chatModel.chatId)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(InternalSeekBarProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    audioPlayerManager?.seekTo(it.progress.toLong())
                }
        )
        compositeDisposable.add(
            RxBus2.listen(PractiseSubmitEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        AppAnalytics.create(AnalyticsEvent.PRACTICE_OPENED.NAME)
                            .addBasicParam()
                            .addUserDetails()
                            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
                            .addParam(
                                AnalyticsEvent.PRACTICE_SOLVED.NAME,
                                (it.chatModel.question != null) && (
                                        it.chatModel.question!!.practiceEngagement.isNullOrEmpty()
                                            .not()
                                        )
                            )
                            .addParam("chatId", it.chatModel.chatId)
                            .push()
                        PractiseSubmitActivity.startPractiseSubmissionActivity(
                            activityRef.get()!!,
                            PRACTISE_SUBMIT_REQUEST_CODE,
                            it.chatModel
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
        compositeDisposable.add(
            RxBus2.listen(GotoChatEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {
                        scrollToPosition(it.chatId)
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listen(AssessmentStartEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        logAssessmentEvent(it.assessmentId)
                        AssessmentActivity.startAssessmentActivity(
                            this,
                            requestCode = ASSESSMENT_REQUEST_CODE,
                            assessmentId = it.assessmentId
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(UnlockNextClassEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        isNewMessageShowing = false
                        conversationBinding.refreshLayout.isRefreshing = true
                        unlockClassViewModel.updateBatchChangeRequest()
                        logUnlockCardEvent()
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listen(ConversationPractiseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        ConversationPracticeActivity.startConversationPracticeActivity(
                            this,
                            CONVERSATION_PRACTISE_REQUEST_CODE,
                            it.id,
                            it.pImage
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(StartCertificationExamEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe(
                    {
                        startActivityForResult(
                            CertificationBaseActivity.certificationExamIntent(
                                this,
                                conversationId = it.conversationId,
                                chatMessageId = it.messageId,
                                certificationId = it.certificationExamId,
                                cExamStatus = it.examStatus,
                                lessonInterval = it.lessonInterval
                            ),
                            CERTIFICATION_REQUEST_CODE
                        )
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenUserProfile::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        it.id?.let { id ->
                            openUserProfileActivity(
                                id,
                                USER_PROFILE_FLOW_FROM.BEST_PERFORMER.value
                            )
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SpecialPracticeEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        Intent(this, SpecialPracticeActivity::class.java).apply {
                            putExtra(SPECIAL_ID, it.specialId)
                            putExtra(CONVERSATION_ID, inboxEntity.conversation_id)
                        }.run {
                            startActivity(this)
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )


        compositeDisposable.add(
            RxBus2.listenWithoutDelay(LessonItemClickEventBus::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        if (inboxEntity.isCourseBought.not() &&
                            inboxEntity.expiryDate != null &&
                            inboxEntity.expiryDate!!.time < System.currentTimeMillis()
                        ) {
                            val nameArr = User.getInstance().firstName?.split(" ")
                            val firstName = if (nameArr != null) nameArr[0] else EMPTY
                            showToast(getFeatureLockedText(inboxEntity.courseId, firstName))
                        } else {
                            MixPanelTracker.publishEvent(MixPanelEvent.LESSON_OPENED)
                                .addParam(ParamKeys.LESSON_ID, it.lessonId)
                                .push()
                            PrefManager.put(IS_FREE_TRIAL, inboxEntity.isCourseBought.not())
                            openedLesson = true
                            startActivityForResult(
                                LessonActivity.getActivityIntent(
                                    this,
                                    it.lessonId,
                                    conversationId = inboxEntity.conversation_id,
                                    isLessonCompleted = it.isLessonCompleted,
                                    testId = AppObjectController.getFirebaseRemoteConfig().getString(
                                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                                    ).toInt(),
                                ),
                                LESSON_REQUEST_CODE
                            )
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listen(AwardItemClickedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(OpenBestPerformerRaceEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        var title: String = EMPTY
                        if (it.isSharable) {
                            title = getString(R.string.my_day_review_title)
                        }

                        it.chatObj?.let { chatObj ->
                            VideoPlayerActivity.getActivityIntentForSharable(
                                this,
                                chatObj,
                                title,
                                chatObj.sharingVideoId.toString(),
                                it.videoUrl,
                                conversationId = inboxEntity.conversation_id,
                                isSharableVideo = true,
                                sharedItem = it.sharedItem
                            )
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun logAssessmentEvent(assessmentId: Int) {
        AppAnalytics.create(AnalyticsEvent.QUIZ_TEST_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.ASSESSMENT_ID.NAME, assessmentId.toString())
            .push()
    }

    private fun logUnlockCardEvent() {
        MixPanelTracker.publishEvent(MixPanelEvent.UNLOCK_NEXT_LESSON)
            .addParam(
                ParamKeys.LESSON_NUMBER,
                conversationAdapter.getLastLesson()?.lessonNo?.plus(1)
            )
            .addParam(ParamKeys.COURSE_NAME, inboxEntity.course_name)
            .push()

        AppAnalytics.create(AnalyticsEvent.UNLOCK_CARD_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .push()
    }

    private fun analyticsAudioPlayed(audioType: AudioType?) {
        AppAnalytics.create(AnalyticsEvent.AUDIO_PLAYED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.AUDIO_ID.NAME, audioType?.id)
            .addParam(AnalyticsEvent.AUDIO_DURATION.NAME, audioType?.duration.toString())
            .addParam(AnalyticsEvent.AUDIO_DOWNLOAD_STATUS.NAME, "Downloaded")
            .addParam(AnalyticsEvent.AUDIO_LOCAL_PATH.NAME, audioType?.downloadedLocalPath)
            .addParam(
                AnalyticsEvent.FLOW_FROM_PARAM.NAME,
                this@ConversationActivity.javaClass.simpleName
            ).push()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == RESULT_OK) {
                val url = data?.data?.path ?: EMPTY

                if (url.isNotBlank()) {
                    addImageMessage(url)
                }
            } else if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE && resultCode == RESULT_OK) {
                showToast(getString(R.string.answer_submitted))
                (data?.getParcelableExtra(PRACTISE_OBJECT) as ChatModel?)?.let {
                    conversationViewModel.refreshMessageObject(it.chatId)
                }
            } else if (requestCode == COURSE_PROGRESS_REQUEST_CODE && data != null) {
                data.getStringExtra(FOCUS_ON_CHAT_ID)?.let {
                    scrollToPosition(it, animation = true)
                }
            } else if (requestCode == VIDEO_OPEN_REQUEST_CODE) {
                (data?.getParcelableExtra(VIDEO_OBJECT) as ChatModel?)?.let {
                    unlockClassViewModel.canWeAddUnlockNextClass(it.chatId)
                }
            } else if (requestCode == VOICE_CALL_REQUEST_CODE) {
                conversationViewModel.getTopicDetail(FREE_TRIAL_CALL_TOPIC_ID)
            } else if (resultCode == RESULT_OK) {
                when (requestCode) {
                    ASSESSMENT_REQUEST_CODE,
                    CERTIFICATION_REQUEST_CODE -> {
                        data?.getStringExtra(CHAT_ROOM_ID)?.let {
                            conversationViewModel.refreshMessageObject(it)
                        }
                    }
                    COURSE_PROGRESS_NEW_REQUEST_CODE -> {
                        data?.getIntExtra(COURSE_ID, -1)?.let {
                            conversationViewModel.refreshLesson(it)
                        }
                    }
                    LESSON_REQUEST_CODE -> {
                        data?.getStringExtra(CHAT_ROOM_ID)?.let {
                            conversationViewModel.refreshMessageObject(it)
                        }
                        if (VoipPref.preferenceManager.getBoolean(IS_FIRST_CALL, true) && openedLesson)
                            showFirstCallBottomSheet()
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun uploadImageByCameraOrGallery() {
        AppAnalytics.create(AnalyticsEvent.CAMERA_CLICKED.NAME).push()
        uploadImageByUser()
    }

    private fun uploadImageByUser() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            ImagePicker.with(this@ConversationActivity)
                                .crop()
                                .cameraOnly()
                                .saveDir(
                                    File(
                                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!,
                                        "ImagePicker"
                                    )
                                )
                                .start(ImagePicker.REQUEST_CODE)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.cameraStoragePermissionPermanentlyDeniedDialog(
                                activityRef.get()!!
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

    override fun onStart() {
        super.onStart()
        try {
            conversationViewModel.singleLiveEvent.observe(this) {
                when (it.what) {
                    COURSE_RESTART_SUCCESS -> {
                        logout()
                        showToast(getString(R.string.course_restart_success))
                    }
                    COURSE_RESTART_FAILURE -> {
                        showToast(getString(R.string.course_restart_fail))
                    }
                    INTERNET_FAILURE -> {
                        showToast(getString(R.string.internet_not_available_msz))
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
        if (PrefManager.getBoolValue(IS_FIRST_TIME_CONVERSATION)) {
            initFreeTrialTimer()
        } else if (PrefManager.hasKey(IS_FIRST_TIME_CONVERSATION).not()) {
            PrefManager.put(IS_FIRST_TIME_CONVERSATION, true)
        }
        PrefManager.getIntValue(CONVERSATION_SCREEN_VISIT_COUNT, defValue = 0).let {
            val count = it.plus(1)
            if (count == 2) {
                conversationViewModel.scheduleMessage(count)
            }
            PrefManager.put(CONVERSATION_SCREEN_VISIT_COUNT, count)
        }
        if (inboxEntity.isCapsuleCourse && inboxEntity.isCourseBought.not())
            showBottomCouponBanner()
        else
            conversationBinding.buyCourseBanner.visibility = GONE
        if (inboxEntity.isCapsuleCourse) {
            utilConversationViewModel.getProfileData(Mentor.getInstance().getId())
        }
        lifecycleScope.launch(Dispatchers.IO) {
            delay(500)
            withContext(Dispatchers.Main) {
                initBottomMargin()
            }
        }
    }

    private fun initBottomMargin() {
        val height = getBottomBannerHeight()
        if ( height >= 0) {
            conversationBinding.chatRv.setPadding(0, 0, 0, height)
            conversationBinding.chatRv.requestLayout()
        }
    }

    fun getBottomBannerHeight(): Int {
        return if (conversationBinding.buyCourseBanner.isVisible) conversationBinding.buyCourseBanner.height else 0
    }

    override fun onPause() {
        super.onPause()
        audioPlayerManager?.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        compositeDisposable.clear()
        readMessageTimerTask?.cancel()
        uiHandler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.cancel()
        AppObjectController.currentPlayingAudioObject = null
        audioPlayerManager?.release()
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        audioPlayerManager?.onPause()
        when {
            conversationBinding.overlayLayout.visibility == VISIBLE -> {
                hideLeaderBoardSpotlight()
                return
            }
            conversationBinding.overlayView.visibility == VISIBLE -> {
                conversationBinding.overlayView.visibility = INVISIBLE
                return
            }
            else -> {
                val resultIntent = Intent()
                setResult(RESULT_OK, resultIntent)
                this@ConversationActivity.finishAndRemoveTask()
            }
        }
    }

    private fun openCourseProgressListingScreen() {
        startActivityForResult(
            CourseProgressActivityNew.getCourseProgressActivityNew(
                this,
                inboxEntity.conversation_id,
                inboxEntity.courseId.toInt()
            ),
            COURSE_PROGRESS_NEW_REQUEST_CODE
        )
    }

    fun visibleItem() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val firstPosition = linearLayoutManager.findFirstVisibleItemPosition()
                val lastPosition = linearLayoutManager.findLastVisibleItemPosition()

                val globalVisibleRect = Rect()
                conversationBinding.chatRv.getGlobalVisibleRect(globalVisibleRect)
                var rowRect: Rect

                for (x in firstPosition..lastPosition step 1) {
                    rowRect = Rect()
                    linearLayoutManager.findViewByPosition(x)!!.getGlobalVisibleRect(rowRect)

                    var percentFirst: Int?
                    percentFirst = if (rowRect.bottom >= globalVisibleRect.bottom) {
                        val visibleHeightFirst = globalVisibleRect.bottom - rowRect.top
                        (visibleHeightFirst * 100) / linearLayoutManager.findViewByPosition(
                            x
                        )!!.height
                    } else {
                        val visibleHeightFirst = rowRect.bottom - globalVisibleRect.top
                        (visibleHeightFirst * 100) / linearLayoutManager.findViewByPosition(
                            x
                        )!!.height
                    }
                    if (percentFirst > VISIBLE_ITEM_PERCENT) {
                        val chatModel = conversationAdapter.getItemAtPosition(lastPosition)
                        chatModel.status = MESSAGE_STATUS.SEEN_BY_USER
                        readChatList.add(chatModel)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun readMessageDatabaseUpdate() {
        readMessageTimerTask = Timer("VisibleMessage", false).scheduleAtFixedRate(5000, 1500) {
            utilConversationViewModel.updateInDatabaseReadMessage(readChatList)
        }
    }

    fun isAudioPlaying(): Boolean {
        return audioPlayerManager?.isPlaying()!!
    }

    private fun setPlayProgress(progress: Int) {
        AppObjectController.currentPlayingAudioObject?.playProgress = progress
        if (currentAudioPosition != -1) {
            conversationAdapter.notifyItemChanged(currentAudioPosition)
        }
    }

    private fun refreshViewAtPos(chatObj: ChatModel) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                delay(500)
                conversationAdapter.updateItem(chatObj)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    override fun onProgressUpdate(progress: Long) {
        setPlayProgress(progress.toInt())
    }

    override fun onPlayerPause() {
        if (currentAudioPosition != -1) {
            conversationAdapter.notifyItemChanged(currentAudioPosition)
        }
    }

    override fun onPlayerResume() {}

    override fun onCurrentTimeUpdated(lastPosition: Long) {}

    override fun onTrackChange(tag: String?) {}

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {}

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onPlayerReleased() {}

    override fun onPlayerEmptyTrack() {}

    override fun complete() {
        audioPlayerManager?.seekTo(0)
        audioPlayerManager?.onPause()
        setPlayProgress(0)
    }

    override fun onSuccessDismiss() {}

    override fun onDismiss() {}

    override fun onDurationUpdate(duration: Long?) {}

    private fun scrollToEnd() {
        lifecycleScope.launch(Dispatchers.Main) {
            linearLayoutManager.scrollToPosition(conversationAdapter.itemCount - 1)
            conversationBinding.scrollToEndButton.visibility = GONE
        }
    }

    private fun scrollToPosition(chatId: String, animation: Boolean = false) {
        lifecycleScope.launch(Dispatchers.Main) {
            val index = conversationAdapter.getMessagePositionById(chatId)
            linearLayoutManager.scrollToPositionWithOffset(index, 40)
            if (animation) {
                conversationAdapter.focusPosition(index)
            }
        }
    }

    fun addAttachmentUIUpdate() {
        AttachmentUtil.revealAttachments(revealAttachmentView, conversationBinding)
        this.revealAttachmentView = !revealAttachmentView
        AppAnalytics.create(AnalyticsEvent.ATTACHMENT_CLICKED.NAME).push()
    }

    private fun sendTextMessage() {
        if (conversationBinding.chatEdit.text.isNullOrEmpty()) {
            return
        }
        if (conversationBinding.chatEdit.text.toString().length > MESSAGE_CHAT_SIZE_LIMIT) {
            showToast(getString(R.string.message_size_limit))
            return
        }
        val message = getTextMessage(
            conversationBinding.chatEdit.text.toString(),
            conversationAdapter.getLastItem()
        )
        conversationAdapter.addMessage(message)
        conversationViewModel.sendTextMessage(
            TChatMessage(conversationBinding.chatEdit.text.toString()),
            chatModel = message
        )
        MixPanelTracker.publishEvent(MixPanelEvent.CHAT_ENTERED)
            .addParam(ParamKeys.CHAT_TEXT, conversationBinding.chatEdit.text.toString())
            .push()
        conversationBinding.chatEdit.setText(EMPTY)
        scrollToEnd()
        AppAnalytics.create(AnalyticsEvent.CHAT_ENTERED.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(
                AnalyticsEvent.CHAT_TEXT.NAME,
                conversationBinding.chatEdit.text.toString()
            )
            .addParam(
                AnalyticsEvent.CHAT_LENGTH.NAME,
                conversationBinding.chatEdit.text.toString().length
            )
    }

    private fun addAudioFromBottomBar(audioFilePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val recordUpdatedPath = AppDirectory.getAudioSentFile(audioFilePath).absolutePath
            AppDirectory.copy(audioFilePath, recordUpdatedPath)
            addAudioAttachment(recordUpdatedPath)
        }
    }

    private fun addRecordedAudioMessage(mediaPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val recordUpdatedPath = AppDirectory.getAudioSentFile(null).absolutePath
            AppDirectory.copy(mediaPath, recordUpdatedPath)
            addAudioAttachment(recordUpdatedPath)
        }
    }

    private fun addAudioAttachment(recordUpdatedPath: String) {
        val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
        val message = getAudioMessage(tAudioMessage, conversationAdapter.getLastItem())
        uiHandler.post {
            conversationAdapter.addMessage(message)
        }
        scrollToEnd()
        conversationViewModel.sendMediaMessage(recordUpdatedPath, tAudioMessage, message)
    }

    private fun addImageMessage(imagePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val imageUpdatedPath = AppDirectory.getImageSentFilePath()
            AppDirectory.copy(imagePath, imageUpdatedPath)
            val tImageMessage = TImageMessage(imageUpdatedPath, imageUpdatedPath)
            val message = getImageMessage(tImageMessage, conversationAdapter.getLastItem())
            uiHandler.post {
                conversationAdapter.addMessage(message)
            }
            scrollToEnd()
            conversationViewModel.sendMediaMessage(imageUpdatedPath, tImageMessage, message)
        }
    }

    private suspend fun setOverlayAnimation() {
        delay(2000)
        withContext(Dispatchers.Main) {
            var i = 0
            while (true) {
                val view = conversationBinding.chatRv.getChildAt(i) ?: break
                if (view.id == R.id.unlock_class_item_container) {
                    val overlayItem = TooltipUtils.getOverlayItemFromView(view)
                    overlayItem?.let {
                        val overlayImageView =
                            conversationBinding.overlayView.findViewById<ImageView>(R.id.card_item_image)
                        val overlayButtonImageView =
                            conversationBinding.overlayView.findViewById<ImageView>(R.id.button_item_image)
                        val unlockBtnView = view.findViewById<MaterialButton>(R.id.btn_start)
                        val overlayButtonItem = TooltipUtils.getOverlayItemFromView(unlockBtnView)
                        overlayImageView.visibility = INVISIBLE
                        overlayButtonImageView.visibility = INVISIBLE
                        conversationBinding.overlayView.setOnClickListener {
                            conversationBinding.overlayView.visibility = INVISIBLE
                        }
                        overlayImageView.setOnClickListener {
                            conversationBinding.overlayView.visibility = INVISIBLE
                        }
                        overlayButtonImageView.setOnClickListener {
                            conversationBinding.overlayView.visibility = INVISIBLE
                            unlockBtnView.performClick()
                        }
                        overlayButtonItem?.let {
                            setOverlayView(
                                overlayItem,
                                overlayImageView,
                                overlayButtonItem,
                                overlayButtonImageView
                            )
                        }
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
        overlayButtonItem: ItemOverlay,
        overlayButtonImageView: ImageView,
    ) {
        val STATUS_BAR_HEIGHT = getStatusBarHeight()
        conversationBinding.overlayView.visibility = INVISIBLE
        conversationBinding.overlayView.setOnClickListener {
            conversationBinding.overlayView.visibility = INVISIBLE
        }
        val arrowView =
            conversationBinding.overlayView.findViewById<ImageView>(R.id.arrow_animation_unlock_class)
        val tooltipView =
            conversationBinding.overlayView.findViewById<JoshTooltip>(R.id.tooltip)
        overlayImageView.setImageBitmap(overlayItem.viewBitmap)
        overlayButtonImageView.setImageBitmap(overlayButtonItem.viewBitmap)
        arrowView.x = getScreenHeightAndWidth().second.div(3).toFloat()
        arrowView.y = overlayButtonItem.y - STATUS_BAR_HEIGHT - resources.getDimension(R.dimen._32sdp)
        overlayImageView.x = overlayItem.x.toFloat()
        overlayImageView.y = overlayItem.y.toFloat() - STATUS_BAR_HEIGHT
        overlayButtonImageView.x = overlayButtonItem.x.toFloat()
        overlayButtonImageView.y = overlayButtonItem.y.toFloat() - STATUS_BAR_HEIGHT
        overlayImageView.requestLayout()
        overlayButtonImageView.requestLayout()
        arrowView.requestLayout()
        conversationBinding.overlayView.visibility = VISIBLE
        arrowView.visibility = VISIBLE
        overlayImageView.visibility = VISIBLE
        overlayButtonImageView.visibility = VISIBLE
        PrefManager.put(HAS_SEEN_UNLOCK_CLASS_ANIMATION, true)
        tooltipView.setTooltipText("Click to Unlock next lesson and learn more.")
        slideInAnimation(tooltipView)
    }

    fun getScreenHeightAndWidth(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels to metrics.widthPixels
    }

    fun slideInAnimation(tooltipView: JoshTooltip) {
        tooltipView.visibility = INVISIBLE
        val start = getScreenHeightAndWidth().second
        val mid = start * 0.02
        tooltipView.x = start.toFloat()
        tooltipView.requestLayout()
        tooltipView.visibility = VISIBLE
        val valueAnimation = ValueAnimator.ofFloat(start.toFloat(), mid.toFloat()).apply {
            interpolator = AccelerateInterpolator()
            duration = 500
            addUpdateListener {
                tooltipView.x = it.animatedValue as Float
                tooltipView.requestLayout()
            }
        }
        valueAnimation.start()
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

    fun getConversationTooltip(): String {
        return getString(R.string.tooltip_conversation)
    }

    private fun showBottomCouponBanner() {
        if (inboxEntity.expiryDate !=null && inboxEntity.expiryDate!!.time < System.currentTimeMillis()) {
            conversationBinding.buyCourseBanner.visibility = GONE
            return
        }
        if (conversationViewModel.repository.isVariantActive(VariantKeys.L2_LESSON_COMPLETE_ENABLED)) {
            conversationViewModel.getCompletedLessonCount(PrefManager.getStringValue(CURRENT_COURSE_ID))
            conversationViewModel.completedLessonCount.observe(this) { count ->
                count?.let {
                    if (count >=
                        AppObjectController.getFirebaseRemoteConfig().getLong(COUPON_UNLOCK_LESSON_COUNT).toInt()
                    ) {
                        conversationViewModel.postGoal(GoalKeys.L2_COUPON_UNLOCKED)
                        conversationBinding.buyCourseBanner.visibility = View.VISIBLE
                        with(conversationBinding) {
                            buyCourseBannerTv.text =
                                AppObjectController.getFirebaseRemoteConfig()
                                    .getString(BUY_COURSE_BANNER_COUPON_UNLOCKED_TEXT)
                                    .replace(
                                        "\$DISCOUNT\$",
                                        AppObjectController.getFirebaseRemoteConfig()
                                            .getLong(LESSON_COMPLETE_COUPON_DISCOUNT)
                                            .toString()
                                    )
                            buyCourseBannerAvailBtn.text = getString(R.string.claim_now)
                            buyCourseBannerAvailBtn.setOnClickListener {
                                conversationViewModel.postGoal(GoalKeys.L2_CLAIM_NOW_CLICKED)
                                BuyPageActivity.startBuyPageActivity(
                                    this@ConversationActivity,
                                    AppObjectController.getFirebaseRemoteConfig()
                                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID),
                                    "CONVERSATION_FT_TIMER",
                                    shouldAutoApplyCoupon = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
