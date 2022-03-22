package com.joshtalks.joshskills.ui.chat

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.google.android.material.button.MaterialButton
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshcamerax.JoshCameraActivity
import com.joshtalks.joshcamerax.utils.ImageQuality
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.Utils.getCurrentMediaVolume
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.decorator.SmoothScrollingLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.extension.setImageWithPlaceholder
import com.joshtalks.joshskills.core.extension.setResourceImageDefault
import com.joshtalks.joshskills.core.extension.shiftGroupChatIconDown
import com.joshtalks.joshskills.core.extension.slideOutAnimation
import com.joshtalks.joshskills.core.interfaces.OnDismissWithSuccess
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.notification.HAS_COURSE_REPORT
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener.State.PAUSED
import com.joshtalks.joshskills.core.service.video_download.VideoDownloadController
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.databinding.FppQuickViewListsItemBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.quizgame.StartActivity
import com.joshtalks.joshskills.quizgame.analytics.GameAnalytics
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.eventbus.*
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.chat_message.TAudioMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.repository.server.chat_message.TVideoMessage
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.activity_feed.ActivityFeedMainActivity
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.chat.adapter.ConversationAdapter
import com.joshtalks.joshskills.ui.chat.service.DownloadMediaService
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.course_progress_new.CourseProgressActivityNew
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.extra.AUTO_START_POPUP
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.fpp.SeeAllRequestsActivity
import com.joshtalks.joshskills.ui.fpp.constants.IS_ACCEPTED
import com.joshtalks.joshskills.ui.fpp.constants.IS_REJECTED
import com.joshtalks.joshskills.ui.fpp.constants.QUICK_VIEW
import com.joshtalks.joshskills.ui.fpp.constants.RECENT_CALL
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.fpp.utils.Blurry
import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics.Event.MAIN_GROUP_ICON
import com.joshtalks.joshskills.ui.leaderboard.ItemOverlay
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_UNLOCK_CLASS_ANIMATION
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.referral.ReferralViewModel
import com.joshtalks.joshskills.ui.subscription.TrialEndBottomSheetFragment
import com.joshtalks.joshskills.ui.tooltip.JoshTooltip
import com.joshtalks.joshskills.ui.tooltip.TooltipUtils
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.video_player.VIDEO_OBJECT
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.joshtalks.joshskills.ui.voip.favorite.FavoriteListActivity
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.StickyHeaderDecoration
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
const val DEFAULT_TOOLTIP_DELAY_IN_MS = 1000L
const val LEADERBOARD_TOOLTIP_DELAY_IN_MS = 1500L

const val PRACTISE_UPDATE_MESSAGE_KEY = "practise_update_message_id"
const val FOCUS_ON_CHAT_ID = "focus_on_chat_id"

private const val TAG = "ConversationActivity"

class ConversationActivity :
    BaseConversationActivity(),
    Player.EventListener,
    ExoAudioPlayer.ProgressUpdateListener,
    AudioPlayerEventListener,
    OnDismissWithSuccess {

    companion object {
        private var unlockOverlayJob: Job? = null

        fun startConversionActivity(activity: Activity, inboxEntity: InboxEntity) {
            val intent = Intent(activity, ConversationActivity::class.java).apply {
                putExtra(CHAT_ROOM_OBJECT, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            activity.startActivity(intent)
        }
    }

    private var buttonClicked = true

    private val rotateOpenAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_open_animation
        )
    }
    private val rotateCloseAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.rotate_close_animation
        )
    }
    private val fromBottomAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.from_bottom_animation
        )
    }
    private val toBottomAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(
            this,
            R.anim.to_bottom_animation
        )
    }
    private var isFirstTime: Boolean = true
    private var requestCountNumber = 0
    private var countdownTimerBack: CountdownTimerBack? = null
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var utilConversationViewModel: UtilConversationViewModel
    private lateinit var unlockClassViewModel: UnlockClassViewModel
    private val conversationAdapter: ConversationAdapter by lazy {
        ConversationAdapter(
            WeakReference(
                this
            )
        )
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
    private var courseProgressUIVisible = false
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

    private val refViewModel: ReferralViewModel by lazy {
        ViewModelProvider(this).get(ReferralViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding = DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.handler = this
        activityRef = WeakReference(this)
        initIntentObject()
        if (::inboxEntity.isInitialized.not()) {
            this.finish()
            return
        }
        init()
    }

    //Setting the animation on the buttons
    private fun setButtonsAnimation() {
        with(conversationBinding) {
            if (!buttonClicked) {
                conversationBinding.imgActivityFeed.startAnimation(fromBottomAnimation)
                conversationBinding.imgGameBtn.startAnimation(fromBottomAnimation)
                conversationBinding.imgGroupChatBtn.startAnimation(fromBottomAnimation)
                conversationBinding.imgFppRequest.startAnimation(fromBottomAnimation)
                floatingActionButtonAdd.startAnimation(rotateOpenAnimation)

            } else {
                conversationBinding.imgActivityFeed.startAnimation(toBottomAnimation)
                conversationBinding.imgGameBtn.startAnimation(toBottomAnimation)
                conversationBinding.imgGroupChatBtn.startAnimation(toBottomAnimation)
                conversationBinding.imgFppRequest.startAnimation(toBottomAnimation)
                floatingActionButtonAdd.startAnimation(rotateCloseAnimation)
            }
        }
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
        // initCourseProgressTooltip()    // course progress tooltip
        initRV()
        initView()
        initFuture()
        addObservable()
        initFreeTrialTimer()
        initABTest()
        fetchMessage()
        readMessageDatabaseUpdate()
        addIssuesToSharedPref()
        if (inboxEntity.isCapsuleCourse) {
            PrefManager.put(CHAT_OPENED_FOR_NOTIFICATION, true)
        }
    }

    private fun initABTest() {
        conversationViewModel.getCampaignData(CampaignKeys.ACTIVITY_FEED.name)
    }

    private fun getAllPendingRequest() {
        conversationViewModel.getPendingRequestsList()
    }

    private fun addIssuesToSharedPref() {
        CoroutineScope(Dispatchers.IO).launch() {

            try {
                PrefManager.putPrefObject(
                    REPORT_ISSUE,
                    AppObjectController.p2pNetworkService.getP2pCallOptions("REPORT")
                )

            } catch (e: java.lang.Exception) {
            }
            try {
                PrefManager.putPrefObject(
                    BLOCK_ISSUE,
                    AppObjectController.p2pNetworkService.getP2pCallOptions("BLOCK")
                )

            } catch (e: java.lang.Exception) {
            }

        }
    }

    private fun initFreeTrialTimer() {
        if (inboxEntity.isCourseBought.not() &&
            inboxEntity.expiryDate != null &&
            inboxEntity.expiryDate!!.time >= System.currentTimeMillis()
        ) {
            conversationBinding.freeTrialContainer.visibility = VISIBLE
            conversationBinding.imgGroupChat.shiftGroupChatIconDown(conversationBinding.txtUnreadCount)
            startTimer(inboxEntity.expiryDate!!.time - System.currentTimeMillis())
        } else if (inboxEntity.isCourseBought.not() &&
            inboxEntity.expiryDate != null &&
            inboxEntity.expiryDate!!.time < System.currentTimeMillis()
        ) {
            PrefManager.put(COURSE_EXPIRY_TIME_IN_MS, inboxEntity.expiryDate!!.time)
            PrefManager.put(IS_COURSE_BOUGHT, inboxEntity.isCourseBought)
            conversationBinding.freeTrialContainer.visibility = VISIBLE
            conversationBinding.imgGroupChat.shiftGroupChatIconDown(conversationBinding.txtUnreadCount)
            conversationBinding.freeTrialText.text = getString(R.string.free_trial_ended)
            conversationBinding.freeTrialExpiryLayout.visibility = VISIBLE
        }
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    conversationBinding.freeTrialText.text = getString(
                        R.string.free_trial_end_in,
                        UtilTime.timeFormatted(millis)
                    )
                }
            }

            override fun onTimerFinish() {
                conversationBinding.freeTrialText.text = getString(R.string.free_trial_ended)
            }
        }
        countdownTimerBack?.startTimer()
    }

    private fun showLessonTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_LESSON_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    conversationBinding.lessonTooltipLayout.visibility = GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (conversationAdapter.getLastLesson()?.lessonNo == 1 &&
                    conversationAdapter.getLastLesson()?.status == LESSON_STATUS.NO
                ) {
                    withContext(Dispatchers.Main) {
                        conversationBinding.lessonTooltipLayout.visibility = VISIBLE
                    }
                } else {
                    PrefManager.put(HAS_SEEN_LESSON_TOOLTIP, true)
                }
            }
        }
    }

    private fun showLeaderBoardTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    conversationBinding.leaderboardTooltipLayout.visibility = GONE
                }
            } else {
                delay(LEADERBOARD_TOOLTIP_DELAY_IN_MS)
                if (conversationAdapter.getLastLesson()?.lessonNo == 1) {
                    withContext(Dispatchers.Main) {
                        conversationBinding.joshTextView.text =
                            leaderboardTooltipList[currentTooltipIndex]
                        conversationBinding.txtTooltipIndex.text =
                            "${currentTooltipIndex + 1} of ${leaderboardTooltipList.size}"
                        conversationBinding.leaderboardTooltipLayout.visibility = VISIBLE
                    }
                }
            }
        }
    }

    fun hideLeaderboardTooltip() {
        conversationBinding.leaderboardTooltipLayout.visibility = GONE
        PrefManager.put(HAS_SEEN_LEADERBOARD_TOOLTIP, true)
    }

    private fun showLeaderBoardSpotlight(hasDelay: Boolean = true) {
        lifecycleScope.launch(Dispatchers.Main) {
            window.statusBarColor = ContextCompat.getColor(
                this@ConversationActivity,
                R.color.leaderboard_overlay_status_bar
            )
            conversationBinding.freeTrialContainer.visibility = GONE
            conversationBinding.overlayLayout.visibility = VISIBLE
            conversationBinding.arrowAnimation.visibility = VISIBLE
            conversationBinding.overlayLeaderboardContainer.visibility = VISIBLE
            //conversationBinding.overlayLeaderboardTooltip.visibility = INVISIBLE
            slideInAnimation(conversationBinding.overlayLeaderboardTooltip)
            /*conversationBinding.overlayLeaderboardTooltip.startAnimation(
                AnimationUtils.loadAnimation(
                    this@ConversationActivity,
                    R.anim.slide_in_right
                )
            )*/
            conversationBinding.labelTapToDismiss.visibility = GONE
            delay(6500)
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

    private fun initEndTrialBottomSheet() {
        TrialEndBottomSheetFragment.showDialog(
            supportFragmentManager
        )
    }

    fun showFreeTrialPaymentScreen() {
        FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
            this,
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            ),
            inboxEntity.expiryDate?.time
        )
        // finish()
    }

    private fun initToolbar() {
        try {
            if (inboxEntity.isCapsuleCourse) {
                PrefManager.put(IS_DEMO_P2P, false)
            }
            conversationBinding.textMessageTitle.text = inboxEntity.course_name
            conversationBinding.imageViewLogo.setImageWithPlaceholder(inboxEntity.course_icon)
            conversationBinding.imageViewLogo.visibility = VISIBLE
            conversationBinding.imageViewLogo.setOnClickListener {
                openCourseProgressListingScreen()
            }
            conversationBinding.textMessageTitle.setOnClickListener {
                openCourseProgressListingScreen()
            }

            conversationBinding.ivBack.setOnClickListener {
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
            conversationBinding.ivIconReferral.setOnClickListener {
                refViewModel.saveImpression(IMPRESSION_REFER_VIA_CONVERSATION_ICON)

                ReferralActivity.startReferralActivity(
                    this@ConversationActivity,
                    ConversationActivity::class.java.name
                )
            }

            conversationBinding.toolbar.inflateMenu(R.menu.conversation_menu)
            profileFeatureActiveView(inboxEntity.isCapsuleCourse)
            showFavtMenuOption(inboxEntity.isCapsuleCourse)
            conversationBinding.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_referral -> {

                        refViewModel.saveImpression(IMPRESSION_REFER_VIA_CONVERSATION_MENU)

                        ReferralActivity.startReferralActivity(
                            this@ConversationActivity,
                            ConversationActivity::class.java.name
                        )
                    }
                    R.id.menu_clear_media -> {
                        clearMediaFromInternal(inboxEntity.conversation_id)
                    }
                    R.id.menu_help -> {
                        openHelpActivity()
                    }
                    R.id.profile_setting -> {
                        openUserProfileActivity(
                            Mentor.getInstance().getId(),
                            USER_PROFILE_FLOW_FROM.MENU.value
                        )
                    }
                    R.id.leaderboard_setting -> {
                        openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
                    }
                    R.id.menu_favorite_list -> {
                        FavoriteListActivity.openFavoriteCallerActivity(
                            this,
                            inboxEntity.conversation_id
                        )
                    }
                }
                return@setOnMenuItemClickListener true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        /*if (reachEndOfData) {
            return
        }*/
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

        conversationBinding.imgActivityFeed.setOnClickListener {
            ActivityFeedMainActivity.startActivityFeedMainActivity(inboxEntity, this)
        }

        conversationBinding.imgFppRequest.setOnClickListener {
            val intent = Intent(this, SeeAllRequestsActivity::class.java)
            startActivity(intent)
        }

        conversationBinding.imgGroupChatBtn.setOnClickListener {
            if (inboxEntity.isCourseBought.not() &&
                inboxEntity.expiryDate != null &&
                inboxEntity.expiryDate!!.time < System.currentTimeMillis()
            ) {
                val nameArr = User.getInstance().firstName?.split(" ")
                val firstName = if (nameArr != null) nameArr[0] else EMPTY
                showToast(getString(R.string.feature_locked, firstName))
            } else {
                val intent = Intent(this, JoshGroupActivity::class.java).apply {
                    putExtra(CONVERSATION_ID, getConversationId())
                }
                GroupAnalytics.push(MAIN_GROUP_ICON)
                startActivity(intent)
            }
        }

        conversationBinding.imgGameBtn.setOnClickListener {
            if (inboxEntity.isCourseBought.not() &&
                inboxEntity.expiryDate != null &&
                inboxEntity.expiryDate!!.time < System.currentTimeMillis()
            ) {
                val nameArr = User.getInstance().firstName?.split(" ")
                val firstName = if (nameArr != null) nameArr[0] else EMPTY
                showToast(getString(R.string.feature_locked, firstName))
            } else {
                val intent = Intent(this, StartActivity::class.java)
                GameAnalytics.push(GameAnalytics.Event.CLICK_ON_MAIN_GAME_ICON)
                startActivity(intent)
            }
        }

        conversationBinding.leaderboardBtnClose.setOnClickListener {
            conversationBinding.userPointContainer.slideOutAnimation(
                conversationBinding.imgGroupChat,
                conversationBinding.txtUnreadCount
            )
            // hideLeaderboardTooltip()
        }

        conversationBinding.leaderboardTxt.setOnClickListener {
            openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
        }
        conversationBinding.overlayLeaderboardContainer.setOnClickListener {
            PrefManager.put(HAS_SEEN_LEADERBOARD_ANIMATION, true)
            openLeaderBoard(inboxEntity.conversation_id, inboxEntity.courseId)
            hideLeaderBoardSpotlight()
        }
        conversationBinding.points.setOnClickListener {
            openUserProfileActivity(
                Mentor.getInstance().getId(),
                USER_PROFILE_FLOW_FROM.FLOATING_BAR.value
            )
        }

        conversationBinding.imgGroupChat.visibility = GONE

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
        conversationBinding.trialClose.setOnClickListener {
            conversationBinding.freeTrialContainer.visibility = View.GONE
            countdownTimerBack?.stop()
        }

        conversationBinding.buyBtn.setOnClickListener {
            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                this,
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                ),
                inboxEntity.expiryDate?.time
            )
            //conversationBinding.freeTrialContainer.visibility = View.GONE
            //countdownTimerBack?.stop()
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < leaderboardTooltipList.size - 1) {
            currentTooltipIndex++
            conversationBinding.joshTextView.text = leaderboardTooltipList[currentTooltipIndex]
            conversationBinding.txtTooltipIndex.text =
                "${currentTooltipIndex + 1} of ${leaderboardTooltipList.size}"
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
        onlyChatView()
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

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
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
        lifecycleScope.launchWhenResumed {
            utilConversationViewModel.unreadMessageCount.collectLatest { count ->
                if (inboxEntity.isGroupActive) {
                    conversationBinding.txtUnreadCount.visibility = VISIBLE
                    //conversationBinding.imgGroupChat.visibility = VISIBLE
                    when {
                        count in 1..99 -> {
                            conversationBinding.txtUnreadCount.text = String.format("%d", count)
                        }
                        count > 99 -> {
                            conversationBinding.txtUnreadCount.text =
                                getString(R.string.max_unread_count)
                        }
                        else -> {
                            conversationBinding.txtUnreadCount.visibility = GONE
                            conversationBinding.txtUnreadCount.text = EMPTY
                        }
                    }
                } else {
                    //conversationBinding.imgGroupChat.visibility = GONE
                    conversationBinding.txtUnreadCount.visibility = GONE
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            utilConversationViewModel.userData.collectLatest { userProfileData ->
                this@ConversationActivity.userProfileData = userProfileData
                conversationBinding.floatingActionButtonAdd.visibility = VISIBLE
                getAllPendingRequest()
                blurViewOnClickListeners(userProfileData)
                initScoreCardView(userProfileData)
                if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE))
                    profileFeatureActiveView(true)
            }
        }

        conversationViewModel.pendingRequestsList.observe(this) {
            with(conversationBinding) {
                if (it.pendingRequestsList.isNullOrEmpty()) {
                    requestCountNumber = 0
                    myRequestsLl.removeAllViews()
                    quickViewNoRequests.visibility = VISIBLE
                    fppRequestCountNumber.visibility = GONE
                    allCountNumber.visibility = GONE
                    viewAllRequests.text =
                        getString(R.string.see_requests, it.pendingRequestsList.size.toString())
                } else {
                    requestCountNumber = it.pendingRequestsList.size
                    quickViewNoRequests.visibility = INVISIBLE
                    allCountNumber.text = it.pendingRequestsList.size.toString()
                    myRequestsLl.visibility = VISIBLE
                    viewAllRequests.text =
                        getString(R.string.see_requests, it.pendingRequestsList.size.toString())
                    horizontalLineForHeading.visibility = VISIBLE
                    var countRequestsList = 0
                    myRequestsLl.removeAllViews()
                    it.pendingRequestsList.forEach {
                        if (countRequestsList < 7) {
                            val view = getPendingRequestItem(it)
                            if (view != null) {
                                conversationBinding.myRequestsLl.addView(view.root)
                                countRequestsList++
                            }
                        }
                    }
                    if (isFirstTime) {
                        isFirstTime = false
                        allCountNumber.visibility = VISIBLE
                    }
                    fppRequestCountNumber.text =
                        it.pendingRequestsList.size.toString()
                }
                viewAllRequests.setOnClickListener {
                    val intent =
                        Intent(conversationBinding.root.context, SeeAllRequestsActivity::class.java)
                    startActivity(intent)
                }
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
                if (it) {
                    conversationViewModel.refreshChatOnManual()
                } else {
                    conversationBinding.refreshLayout.isRefreshing = false
                }
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
                activityFeedControl =
                    (map.variantKey == VariantKeys.ACTIVITY_FEED_ENABLED.name) && map.variableMap?.isEnabled == true
            }
            if (activityFeedControl) conversationBinding.imgActivityFeed .visibility =
                VISIBLE else conversationBinding.imgActivityFeed.visibility = GONE
        }
    }

    private fun blurViewOnClickListeners(userProfileData: UserProfileResponse) {
        conversationBinding.floatingActionButtonAdd.setOnClickListener {
            setExpandableButtons(userProfileData)
            setButtonsAnimation()
        }

        conversationBinding.blurView.setOnClickListener {
            setExpandableButtons(userProfileData)
            setButtonsAnimation()
        }
    }


    private fun setExpandableButtons(userProfileData: UserProfileResponse) {
        with(conversationBinding) {
            if (buttonClicked) {
                getAllPendingRequest()
                conversationBinding.root.setOnClickListener {}
                showBlurOrQuickView()
                imgActivityFeed.visibility = VISIBLE
                imgFppRequest.visibility = VISIBLE

                if (userProfileData.isGameActive)
                    imgGameBtn.visibility = VISIBLE

                if (userProfileData.hasGroupAccess)
                    imgGroupChatBtn.visibility = VISIBLE
            } else {
                conversationBinding.root.onFocusChangeListener = null
                hideBlurOrQuickView()
                imgActivityFeed.visibility = GONE
                imgFppRequest.visibility = GONE

                if (userProfileData.isGameActive)
                    imgGameBtn.visibility = GONE

                if (userProfileData.hasGroupAccess)
                    imgGroupChatBtn.visibility = GONE
            }
        }
    }

    private fun showBlurOrQuickView() {
        conversationBinding.allCountNumber.visibility = GONE
        conversationBinding.userPointContainer.elevation = 0f
        conversationBinding.imgMain.visibility = VISIBLE
        conversationBinding.imgPointer.visibility = VISIBLE
        conversationBinding.root.setOnClickListener { }
        lifecycleScope.launchWhenCreated {
            conversationBinding.blurView.visibility = VISIBLE
            Blurry.with(this@ConversationActivity).radius(25).sampling(3)
                .onto(conversationBinding.blurView, conversationBinding.rootView)
        }

        buttonClicked = false
        conversationBinding.quickCardView.visibility = VISIBLE

        if (requestCountNumber > 0) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(200)
                conversationBinding.fppRequestCountNumber.visibility = VISIBLE
            }
        }
    }

    private fun hideBlurOrQuickView() {
        conversationBinding.root.setOnClickListener(null)
        Blurry.delete(conversationBinding.blurView)
        conversationBinding.imgPointer.visibility = INVISIBLE
        conversationBinding.userPointContainer.elevation = 3f
        conversationBinding.blurView.visibility = GONE
        buttonClicked = true
        conversationBinding.quickCardView.visibility = GONE
        conversationBinding.imgMain.visibility = GONE
        conversationBinding.imgMain.setOnClickListener(null)
        conversationBinding.root.onFocusChangeListener = null
        conversationBinding.fppRequestCountNumber.visibility = GONE
        if (requestCountNumber > 0) {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(350)
                conversationBinding.allCountNumber.visibility = VISIBLE
            }
        }

    }


    private fun getPendingRequestItem(pendingRequestDetail: PendingRequestDetail): FppQuickViewListsItemBinding {
        val view =
            FppQuickViewListsItemBinding.inflate(
                LayoutInflater.from(conversationBinding.root.context),
                conversationBinding.root,
                false
            )
        with(view) {
            itemData = pendingRequestDetail
            fppRequestContainer.setOnClickListener {
                openUserProfileActivity(
                    pendingRequestDetail.senderMentorId ?: "",
                    RECENT_CALL
                )
            }
            profileImage.setUserImageOrInitials(
                pendingRequestDetail.photoUrl ?: "",
                pendingRequestDetail.fullName ?: ""
            )
            btnConfirmRequest.setOnClickListener {
                btnConfirmRequest.visibility = GONE
                btnNotNow.visibility = GONE
                tvSpokenTime.text = getString(R.string.now_fpp)
                fppRequestContainer.setBackgroundColor(resources.getColor(R.color.request_respond))
                conversationViewModel.confirmOrRejectFppRequest(
                    pendingRequestDetail.senderMentorId!!,
                    IS_ACCEPTED, QUICK_VIEW
                )
            }
            btnNotNow.setOnClickListener {
                btnConfirmRequest.visibility = GONE
                btnNotNow.visibility = GONE
                tvSpokenTime.text = getString(R.string.request_removed)
                fppRequestContainer.setBackgroundColor(resources.getColor(R.color.request_respond))
                conversationViewModel.confirmOrRejectFppRequest(
                    pendingRequestDetail.senderMentorId!!,
                    IS_REJECTED, QUICK_VIEW
                )

            }
        }
        return view
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
        if (showMenu) {
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isVisible = true
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isEnabled = true
        } else {
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isVisible = false
            conversationBinding.toolbar.menu.findItem(R.id.menu_favorite_list).isEnabled = false
        }
    }

    private fun initScoreCardView(userData: UserProfileResponse) {
        userData.isContainerVisible?.let { isLeaderBoardActive ->
            if (isLeaderBoardActive) {
                conversationBinding.points.text = userData.points.toString().plus(" Points")
                //conversationBinding.imgGroupChat.shiftGroupChatIconDown(conversationBinding.txtUnreadCount)
                // conversationBinding.userPointContainer.slideInAnimation()
                conversationBinding.userPointContainer.visibility = VISIBLE
                // showLeaderBoardTooltip()
                if (!PrefManager.getBoolValue(HAS_SEEN_LEADERBOARD_ANIMATION))
                    showLeaderBoardSpotlight()
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        val status =
                            AppObjectController.appDatabase.lessonDao().getLessonStatus(1)
                        Log.d(TAG, "initScoreCardView: $status")
                        withContext(Dispatchers.Main) {
                            if (status == LESSON_STATUS.CO && !PrefManager.getBoolValue(
                                    HAS_SEEN_UNLOCK_CLASS_ANIMATION
                                )
                            ) {
                                delay(1000)
                                setOverlayAnimation()
                            } else if (PrefManager.getBoolValue(
                                    SHOULD_SHOW_AUTOSTART_POPUP,
                                    defValue = true
                                )
                                && System.currentTimeMillis()
                                    .minus(PrefManager.getLongValue(LAST_TIME_AUTOSTART_SHOWN)) > 259200000L
                            ) {
                                PrefManager.put(
                                    LAST_TIME_AUTOSTART_SHOWN,
                                    System.currentTimeMillis()
                                )
                                checkForOemNotifications(AUTO_START_POPUP)
                            }
                        }
                    }
                }
            } else {
                conversationBinding.userPointContainer.visibility = GONE
                //conversationBinding.imgGroupChat.shiftGroupChatIconUp(conversationBinding.txtUnreadCount)
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
                        if (it.state == com.google.android.exoplayer2.offline.Download.STATE_COMPLETED) {
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
                                                AppObjectController.videoDownloadTracker.download(
                                                    it.chatModel,
                                                    Uri.parse(it.url),
                                                    VideoDownloadController.getInstance()
                                                        .buildRenderersFactory(true)
                                                )
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
                        else -> {
                        }
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
                        // conversationBinding.chatRv.removeView(it.viewHolder)
                        // conversationAdapter.removeNewClassCard()
                        conversationAdapter.removeUnlockMessage()
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
                            showToast(getString(R.string.feature_locked, firstName))
                        } else {
                            startActivityForResult(
                                LessonActivity.getActivityIntent(
                                    this,
                                    it.lessonId,
                                    conversationId = inboxEntity.conversation_id,
                                    isNewGrammar = it.isNewGrammar,
                                    isLessonCompleted = it.isLessonCompleted,
                                    isFreeTrail = inboxEntity.isCourseBought.not()
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
                .subscribe(
                    {
//                        showAward(listOf(it.award), true)
                    },
                    {
                        it.printStackTrace()
                    }
                )
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
        AppAnalytics.create(AnalyticsEvent.UNLOCK_CARD_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .push()
    }

    private fun analyticsAudioPlayed(
        audioType: AudioType?
    ) {
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
            if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    when {
                        intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS) -> {
                            intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)
                                ?.getOrNull(0)?.let {
                                    if (it.isNotBlank()) {
                                        addImageMessage(it)
                                    }
                                }
                        }
                        intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS) -> {
                            val videoPath =
                                intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                            videoPath?.let {
                                addVideoMessage(it)
                            }
                        }
                        else -> return
                    }
                }
            } else if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
            } else if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    ASSESSMENT_REQUEST_CODE,
                    LESSON_REQUEST_CODE,
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
                            val options = Options.init()
                                .setRequestCode(IMAGE_SELECT_REQUEST_CODE)
                                .setCount(1)
                                .setFrontfacing(false)
                                .setPath(AppDirectory.getTempPath())
                                .setImageQuality(ImageQuality.HIGH)
                                .setScreenOrientation(Options.SCREEN_ORIENTATION_PORTRAIT)

                            JoshCameraActivity.startJoshCameraxActivity(
                                this@ConversationActivity,
                                options
                            )
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

    override fun onResume() {
        super.onResume()
        subscribeRXBus()

        if (inboxEntity.isCapsuleCourse) {
            utilConversationViewModel.getProfileData(Mentor.getInstance().getId())
        }
    }

    override fun onPause() {
        super.onPause()
        audioPlayerManager?.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        hideBlurOrQuickView()
        conversationBinding.imgMain.visibility = GONE
        setButtonsAnimation()
        compositeDisposable.clear()
        readMessageTimerTask?.cancel()
        uiHandler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
        AppObjectController.currentPlayingAudioObject = null
        audioPlayerManager?.release()
    }

    override fun onBackPressed() {
        audioPlayerManager?.onPause()
        if (conversationBinding.overlayLayout.visibility == VISIBLE) {
            hideLeaderBoardSpotlight()
        } else if (conversationBinding.overlayView.visibility == VISIBLE)
            conversationBinding.overlayView.visibility = View.INVISIBLE
        else {
            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            this@ConversationActivity.finishAndRemoveTask()
        }
    }

    private fun openCourseProgressListingScreen() {
        val isLessonTypeChat = inboxEntity.isCapsuleCourse
        if (isLessonTypeChat) {
            startActivityForResult(
                CourseProgressActivityNew.getCourseProgressActivityNew(
                    this,
                    inboxEntity.conversation_id,
                    inboxEntity.courseId.toInt()
                ),
                COURSE_PROGRESS_NEW_REQUEST_CODE
            )
            PrefManager.put(COURSE_PROGRESS_OPENED, true)
            courseProgressUIVisible = true
        } else {
            AppAnalytics.create(AnalyticsEvent.COURSE_PROGRESS_OVERVIEW.NAME).push()
            CourseProgressActivity.startCourseProgressActivity(
                this,
                COURSE_PROGRESS_REQUEST_CODE,
                inboxEntity
            )
        }
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
//  ex.printStackTrace()
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

    private fun refreshView(chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatObj = AppObjectController.appDatabase.chatDao()
                    .getUpdatedChatObjectViaId(chatId)
                refreshViewAtPos(chatObj)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
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

    private fun addVideoMessage(videoPath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val videoSentFile = AppDirectory.videoSentFile()
            AppDirectory.copy(videoPath, videoSentFile.absolutePath)
            val tVideoMessage =
                TVideoMessage(videoSentFile.absolutePath, videoSentFile.absolutePath)
            val message = getVideoMessage(tVideoMessage, conversationAdapter.getLastItem())
            uiHandler.post {
                conversationAdapter.addMessage(message)
            }
            scrollToEnd()
            conversationViewModel.sendMediaMessage(
                videoSentFile.absolutePath,
                tVideoMessage,
                message
            )
        }
    }

    private suspend fun setOverlayAnimation() {
        delay(1000)
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
                        val overlayButtonItem =
                            TooltipUtils.getOverlayItemFromView(unlockBtnView)
                        overlayImageView.visibility = View.INVISIBLE
                        overlayButtonImageView.visibility = View.INVISIBLE
                        conversationBinding.overlayView.setOnClickListener {
                            conversationBinding.overlayView.visibility = View.INVISIBLE
                        }
                        overlayImageView.setOnClickListener {
                            conversationBinding.overlayView.visibility = View.INVISIBLE
                        }
                        overlayButtonImageView.setOnClickListener {
                            conversationBinding.overlayView.visibility = View.INVISIBLE
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
        conversationBinding.overlayView.visibility = View.INVISIBLE
        conversationBinding.overlayView.setOnClickListener {
            conversationBinding.overlayView.visibility = View.INVISIBLE
        }
        val arrowView =
            conversationBinding.overlayView.findViewById<ImageView>(R.id.arrow_animation_unlock_class)
        val tooltipView =
            conversationBinding.overlayView.findViewById<JoshTooltip>(R.id.tooltip)
        overlayImageView.setImageBitmap(overlayItem.viewBitmap)
        overlayButtonImageView.setImageBitmap(overlayButtonItem.viewBitmap)
        arrowView.x =
            overlayButtonItem.x.toFloat() - resources.getDimension(R.dimen._40sdp) + (overlayButtonImageView.width / 2.0).toFloat() - resources.getDimension(
                R.dimen._45sdp
            )
        arrowView.y =
            overlayButtonItem.y - STATUS_BAR_HEIGHT - resources.getDimension(R.dimen._32sdp)
        overlayImageView.x = overlayItem.x.toFloat()
        overlayImageView.y = overlayItem.y.toFloat() - STATUS_BAR_HEIGHT
        overlayButtonImageView.x = overlayButtonItem.x.toFloat()
        overlayButtonImageView.y = overlayButtonItem.y.toFloat() - STATUS_BAR_HEIGHT
        overlayImageView.requestLayout()
        overlayButtonImageView.requestLayout()
        arrowView.requestLayout()
        conversationBinding.overlayView.visibility = View.VISIBLE
        arrowView.visibility = VISIBLE
        overlayImageView.visibility = View.VISIBLE
        overlayButtonImageView.visibility = View.VISIBLE
        PrefManager.put(HAS_SEEN_UNLOCK_CLASS_ANIMATION, true)
        tooltipView.setTooltipText("बस ना? नहीं अभी भी नहीं. और तेज़ी से आगे बड़ने के लिए आप कल का lesson भी अभी कर सकते हैं")
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
        val mid = start * 0.2 * -1
        val end = tooltipView.x
        tooltipView.x = start.toFloat()
        tooltipView.requestLayout()
        tooltipView.visibility = VISIBLE
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

    fun getStatusBarHeight(): Int {
        val rectangle = Rect()
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle)
        val statusBarHeight = rectangle.top
        val contentViewTop: Int = window.findViewById<View>(Window.ID_ANDROID_CONTENT).getTop()
        val titleBarHeight = contentViewTop - statusBarHeight
        Log.d(TAG, "getStatusBarHeight: $titleBarHeight")
        return if (titleBarHeight < 0) titleBarHeight * -1 else titleBarHeight
    }
}
