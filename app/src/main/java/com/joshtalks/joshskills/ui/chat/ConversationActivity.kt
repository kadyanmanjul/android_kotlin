package com.joshtalks.joshskills.ui.chat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.cometchat.pro.constants.CometChatConstants
import com.facebook.share.internal.ShareConstants
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greentoad.turtlebody.mediapicker.MediaPicker
import com.greentoad.turtlebody.mediapicker.core.MediaPickerConfig
import com.joshtalks.joshcamerax.JoshCameraActivity
import com.joshtalks.joshcamerax.utils.ImageQuality
import com.joshtalks.joshcamerax.utils.Options
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CERTIFICATE_GENERATE
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_ENDED
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_STARTED
import com.joshtalks.joshskills.core.MESSAGE_CHAT_SIZE_LIMIT
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REMAINING_TRIAL_DAYS
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.Utils.getCurrentMediaVolume
import com.joshtalks.joshskills.core.alphaAnimation
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.core.custom_ui.JoshSnackBar
import com.joshtalks.joshskills.core.custom_ui.SnappingLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.interfaces.OnDismissWithSuccess
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.notification.HAS_COURSE_REPORT
import com.joshtalks.joshskills.core.notification.QUESTION_ID
import com.joshtalks.joshskills.core.playback.PlaybackInfoListener.State.PAUSED
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivityConversationBinding
import com.joshtalks.joshskills.messaging.MessageBuilderFactory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.eventbus.AssessmentStartEventBus
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DeleteMessageEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadCompletedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.DownloadMediaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.GotoChatEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.repository.local.eventbus.InternalSeekBarProgressEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MediaProgressEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MessageCompleteEventBus
import com.joshtalks.joshskills.repository.local.eventbus.P2PStartEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PdfOpenEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PlayVideoEvent
import com.joshtalks.joshskills.repository.local.eventbus.PractiseSubmitEventBus
import com.joshtalks.joshskills.repository.local.eventbus.StartCertificationExamEventBus
import com.joshtalks.joshskills.repository.local.eventbus.UnlockNextClassEventBus
import com.joshtalks.joshskills.repository.local.eventbus.VideoDownloadedBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.NotificationAction
import com.joshtalks.joshskills.repository.server.chat_message.TAudioMessage
import com.joshtalks.joshskills.repository.server.chat_message.TChatMessage
import com.joshtalks.joshskills.repository.server.chat_message.TImageMessage
import com.joshtalks.joshskills.repository.server.chat_message.TUnlockClassMessage
import com.joshtalks.joshskills.repository.server.chat_message.TVideoMessage
import com.joshtalks.joshskills.repository.server.groupchat.GroupDetails
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.certification_exam.CertificationBaseActivity
import com.joshtalks.joshskills.ui.chat.extra.CallingFeatureShowcaseView
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeActivity
import com.joshtalks.joshskills.ui.courseprogress.CourseProgressActivity
import com.joshtalks.joshskills.ui.day_wise_course.DayWiseCourseActivity
import com.joshtalks.joshskills.ui.day_wise_course.lesson.LessonViewHolder
import com.joshtalks.joshskills.ui.extra.ImageShowFragment
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.practise.PRACTISE_OBJECT
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity
import com.joshtalks.joshskills.ui.referral.ReferralActivity
import com.joshtalks.joshskills.ui.subscription.TrialEndBottomSheetFragment
import com.joshtalks.joshskills.ui.video_player.IS_BATCH_CHANGED
import com.joshtalks.joshskills.ui.video_player.LAST_VIDEO_INTERVAL
import com.joshtalks.joshskills.ui.video_player.NEXT_VIDEO_AVAILABLE
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.AssessmentViewHolder
import com.joshtalks.joshskills.ui.view_holders.AudioPlayerViewHolder
import com.joshtalks.joshskills.ui.view_holders.BaseCell
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.joshtalks.joshskills.ui.view_holders.CertificationExamViewHolder
import com.joshtalks.joshskills.ui.view_holders.ConversationPractiseViewHolder
import com.joshtalks.joshskills.ui.view_holders.ImageViewHolder
import com.joshtalks.joshskills.ui.view_holders.NewMessageViewHolder
import com.joshtalks.joshskills.ui.view_holders.P2PViewHolder
import com.joshtalks.joshskills.ui.view_holders.PdfViewHolder
import com.joshtalks.joshskills.ui.view_holders.PracticeViewHolder
import com.joshtalks.joshskills.ui.view_holders.TextViewHolder
import com.joshtalks.joshskills.ui.view_holders.TimeViewHolder
import com.joshtalks.joshskills.ui.view_holders.UnlockNextClassViewHolder
import com.joshtalks.joshskills.ui.view_holders.VideoViewHolder
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.recordview.CustomImageButton.FIRST_STATE
import com.joshtalks.recordview.CustomImageButton.SECOND_STATE
import com.joshtalks.recordview.OnRecordListener
import com.joshtalks.recordview.OnRecordTouchListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import constant.StringContract
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import screen.messagelist.CometChatMessageListActivity
import java.lang.ref.WeakReference
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.component1
import kotlin.collections.component2
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

const val PRACTISE_UPDATE_MESSAGE_KEY = "practise_update_message_id"
const val FOCUS_ON_CHAT_ID = "focus_on_chat_id"


class ConversationActivity : CoreJoshActivity(), Player.EventListener,
    ExoAudioPlayer.ProgressUpdateListener, AudioPlayerEventListener, OnDismissWithSuccess {

    companion object {
        fun startConversionActivity(activity: Activity, inboxEntity: InboxEntity) {
            Intent(activity, ConversationActivity::class.java).apply {
                putExtra(CHAT_ROOM_OBJECT, inboxEntity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.run {
                activity.startActivity(this)
            }
        }
    }

    private var currentAudioPosition: Int = -1
    private val conversationViewModel: ConversationViewModel by lazy {
        ViewModelProvider(this).get(ConversationViewModel::class.java)
    }
    private lateinit var conversationBinding: ActivityConversationBinding
    private lateinit var inboxEntity: InboxEntity
    private lateinit var activityRef: WeakReference<FragmentActivity>
    private lateinit var linearLayoutManager: SnappingLinearLayoutManager
    private lateinit var internetAvailableStatus: Snackbar
    private val cMessageType: BASE_MESSAGE_TYPE = BASE_MESSAGE_TYPE.TX
    private val compositeDisposable = CompositeDisposable()
    private var revealAttachmentView: Boolean = false
    private val conversationList: MutableList<ChatModel> = ArrayList()
    private var removingConversationList = linkedSetOf<ChatModel>()
    private val readChatList: MutableSet<ChatModel> = mutableSetOf()
    private var readMessageTimerTask: TimerTask? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private var audioPlayerManager: ExoAudioPlayer? = null
    private var isOnlyChat = false
    private var isNewChatViewAdd = true
    private var lastDay: Date = Date()
    private var chatModelLast: ChatModel? = null
    private var lastMessage: ChatModel? = null
    private var internetAvailableFlag: Boolean = true
    private var flowFrom: String? = EMPTY
    private var unlockViewHolder: UnlockNextClassViewHolder? = null
    private var lastVideoStartingDate: Date? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conversationBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_conversation)
        conversationBinding.handler = this
        activityRef = WeakReference(this)

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
            notificationActionProcess()
        }
        if (intent.hasExtra(HAS_COURSE_REPORT)) {
            openCourseProgressListingScreen()
        }
        if (intent.hasExtra(FOCUS_ON_CHAT_ID)) {
            intent.getParcelableExtra<ChatModel>(FOCUS_ON_CHAT_ID)?.chatId?.run {
                scrollToPosition(this)
            }
        }
        super.processIntent(intent)
        checkInboxModel()
        conversationBinding.viewmodel = initViewModel()
        init()
    }

    override fun onNewIntent(mIntent: Intent) {
        intent = mIntent
        super.processIntent(mIntent)
        if (intent.hasExtra(UPDATED_CHAT_ROOM_OBJECT)) {
            flowFrom = "Notification"
            val temp = intent.getParcelableExtra(UPDATED_CHAT_ROOM_OBJECT) as InboxEntity?
            if (temp == null) {
                this.finish()
            }
            temp?.let { inboxObj ->
                try {
                    val tempIn: InboxEntity? = inboxEntity
                    if (tempIn?.conversation_id != inboxObj.conversation_id) {
                        this.inboxEntity = inboxObj
                    }
                } catch (ex: Exception) {
                    this.finish()
                    ex.printStackTrace()
                }
                checkInboxModel()
                initViewModel()
                fetchMessage()
            }
        }
        if (intent.hasExtra(HAS_COURSE_REPORT)) {
            openCourseProgressListingScreen()
        }
        if (intent.hasExtra(FOCUS_ON_CHAT_ID)) {
            mIntent.getParcelableExtra<ChatModel>(FOCUS_ON_CHAT_ID)?.chatId?.run {
                scrollToPosition(this)
            }
        }
        notificationActionProcess()
        super.onNewIntent(mIntent)
    }

    private fun checkInboxModel() {
        try {
            if (::inboxEntity.isInitialized.not()) {
                this.finish()
            }
        } catch (ex: Exception) {
            this.finish()
        }

    }

    private fun initViewModel(): ConversationViewModel {
        try {
            this.conversationViewModel.inboxEntity = this.inboxEntity
            this.conversationBinding.viewmodel = conversationViewModel
            this.conversationBinding.lifecycleOwner = this
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return this.conversationViewModel
    }


    private fun init() {
        AppAnalytics.create(AnalyticsEvent.COURSE_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, flowFrom)
            .push()
        initSnackBar()
        setToolbar()
        initRV()
        liveDataObservable()
        initView()
        refreshChat()
        audioPlayerManager = ExoAudioPlayer.getInstance()
        audioPlayerManager?.setProgressUpdateListener(this)
        audioPlayerManager?.playerListener = this
        fetchMessage()
        uiHandler.postDelayed({
            hideProgressBar()
        }, 5000)
    }

    private fun fetchMessage() {
        showProgressBar()
        conversationViewModel.getAllUserMessage()
        onlyChatView()
        if (inboxEntity.report_status && PrefManager.hasKey(
                inboxEntity.conversation_id.trim().plus(CERTIFICATE_GENERATE)
            ).not()
        ) {
            alphaAnimation(findViewById(R.id.ic_notification_dot))
        }
    }

    private fun refreshChat() {
        conversationBinding.refreshLayout.setOnRefreshListener {
            if (Utils.isInternetAvailable()) {
                conversationBinding.refreshLayout.isRefreshing = true
                conversationViewModel.refreshChatOnManual()
            } else {
                conversationBinding.refreshLayout.isRefreshing = false
            }

        }
    }

    private fun setToolbar() {
        try {
            if (inboxEntity.voiceCallStatus) {
                conversationBinding.ivCall.visibility = VISIBLE
                CallingFeatureShowcaseView.newInstance()
                    .show(supportFragmentManager, "calling_start_mediator_dialog")
            }
            conversationBinding.textMessageTitle.text = inboxEntity.course_name
            conversationBinding.imageViewLogo.setImageResource(R.drawable.ic_josh_course)
            inboxEntity.course_icon?.let {
                Glide.with(applicationContext)
                    .load(it)
                    .override(Target.SIZE_ORIGINAL)
                    .optionalTransform(
                        WebpDrawable::class.java,
                        WebpDrawableTransformation(CircleCrop())
                    )
                    .into(conversationBinding.imageViewLogo)
            }
            conversationBinding.imageViewLogo.visibility = VISIBLE
            conversationBinding.imageViewLogo.setOnClickListener {
                openCourseProgressListingScreen()
            }
            conversationBinding.textMessageTitle.setOnClickListener {
                openCourseProgressListingScreen()
            }

            conversationBinding.ivBack.setOnClickListener {
                finish()
            }
            conversationBinding.toolbar.inflateMenu(R.menu.conversation_menu)
            conversationBinding.toolbar.setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.groupChat -> {
                        conversationViewModel.initCometChat()
                    }
                    R.id.menu_referral -> {
                        ReferralActivity.startReferralActivity(
                            this@ConversationActivity,
                            ConversationActivity::class.java.name
                        )
                    }
                    R.id.menu_clear_media -> {
                        clearMediaFromInternal()
                    }
                    R.id.menu_help -> {
                        openHelpActivity()
                    }
                }
                return@setOnMenuItemClickListener true
            }
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
        }
    }

    fun practiseOnCall() {
        RxBus2.publish(P2PStartEventBus())
    }

    private fun initSnackBar() {
        internetAvailableStatus = JoshSnackBar.builder().setActivity(this)
            .setBackgroundColor(ContextCompat.getColor(application, R.color.white))
            .setActionText("Please enable")
            .setDuration(JoshSnackBar.LENGTH_INDEFINITE)
            .setTextSize(14f)
            .setTextColor(ContextCompat.getColor(application, R.color.gray_79))
            .setText(getString(R.string.internet_not_available_msz))
            .setMaxLines(1)
            .setActionTextColor(ContextCompat.getColor(application, R.color.action_color))
            .setActionTextSize(12f)
            .setActionClickListener {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            .build()
    }

    private fun onlyChatView() {
        inboxEntity.chat_type?.run {
            when {
                this.equals("NC", ignoreCase = true) -> {
                    conversationBinding.flAttachment.visibility = GONE
                    conversationBinding.quickToggle.visibility = GONE
                    conversationBinding.recordButton.visibility = INVISIBLE
                    conversationBinding.attachmentContainer.visibility = GONE
                    isOnlyChat = true
                    conversationBinding.messageButton.visibility = VISIBLE
                    conversationBinding.messageButton.setImageResource(R.drawable.ic_send)
                    Glide.with(applicationContext)
                        .load(R.drawable.ic_send)
                        .override(Target.SIZE_ORIGINAL)
                        .into(conversationBinding.messageButton)
                }
                this.equals("RC", ignoreCase = true) -> {
                    conversationBinding.bottomBar.visibility = GONE
                }
                else -> {
                }
            }
        }
    }


    private fun initRV() {
        linearLayoutManager = SnappingLinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.isSmoothScrollbarEnabled = true
        conversationBinding.chatRv.builder
            .setHasFixedSize(false)
            .setLayoutManager(linearLayoutManager)
        conversationBinding.chatRv.itemAnimator = null
        conversationBinding.chatRv.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    this,
                    4f
                )
            )
        )
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

        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        conversationBinding.recordButton.setRecordView(conversationBinding.recordView)
        conversationBinding.recordView.cancelBounds = 2f
        conversationBinding.recordView.setSmallMicColor(Color.parseColor("#c2185b"))
        conversationBinding.recordView.setLessThanSecondAllowed(false)
        conversationBinding.recordView.setSlideToCancelText(getString(R.string.slide_to_cancel))
        conversationBinding.recordView.setCustomSounds(
            R.raw.record_start,
            R.raw.record_finished,
            0
        )
        conversationBinding.recordButton.isListenForRecord =
            PermissionUtils.checkPermissionForAudioRecord(this@ConversationActivity)
        conversationBinding.recordView.setOnRecordListener(object : OnRecordListener {
            override fun onStart() {
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
                        addUploadAudioMedia(it.absolutePath)
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

        conversationBinding.recordButton.setOnTouchListener(OnRecordTouchListener {
            if (conversationBinding.chatEdit.text.toString()
                    .isEmpty() && it == MotionEvent.ACTION_DOWN
            ) {
                if (PermissionUtils.isAudioAndStoragePermissionEnable(this).not()) {
                    PermissionUtils.audioRecordStorageReadAndWritePermission(activityRef.get()!!,
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
                        })
                } else {
                    conversationBinding.recordButton.isListenForRecord = true
                }
            }
        })

        conversationBinding.recordButton.setOnRecordClickListener {
            sendTextMessage()
        }

        conversationBinding.messageButton.setOnClickListener {
            sendTextMessage()
        }

        findViewById<View>(R.id.ll_audio).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.AUDIO_SELECTED.NAME).push()
            uploadAttachment()

            PermissionUtils.storageReadAndWritePermission(activityRef.get()!!,
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
                })


        }
        findViewById<View>(R.id.ll_camera).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.CAMERA_SELECTED.NAME).push()
            uploadAttachment()
            uploadImageByUser()

        }
        conversationBinding.scrollToEndButton.setOnClickListener {
            scrollToEnd()
        }
    }

    private fun sendTextMessage() {
        if (conversationBinding.chatEdit.text.isNullOrEmpty()) {
            return
        }
        if (conversationBinding.chatEdit.text.toString().length > MESSAGE_CHAT_SIZE_LIMIT) {
            Toast.makeText(
                applicationContext,
                getString(R.string.message_size_limit),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (cMessageType == BASE_MESSAGE_TYPE.TX) {
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
            val tChatMessage =
                TChatMessage(conversationBinding.chatEdit.text.toString())
            val cell = MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.TX,
                tChatMessage,
                getLastMessageObject()
            )
            conversationBinding.chatRv.addView(cell)
            scrollToEnd()
            conversationViewModel.sendTextMessage(
                TChatMessage(conversationBinding.chatEdit.text.toString()),
                chatModel = cell.message
            )
        }
        conversationBinding.chatEdit.setText(EMPTY)
    }

    @SuppressLint("CheckResult")
    private fun bottomAudioAttachment() {
        uploadAttachment()
        val pickerConfig = MediaPickerConfig()
            .setUriPermanentAccess(true)
            .setAllowMultiSelection(false)
            .setScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        MediaPicker.with(this, MediaPicker.MediaTypes.AUDIO)
            .setConfig(pickerConfig)
            .setFileMissingListener(object :
                MediaPicker.MediaPickerImpl.OnMediaListener {
                override fun onMissingFileWarning() {
                }
            })
            .onResult()
            .subscribe({
                it?.let {
                    it[0].path?.let { path ->
                        AppAnalytics.create(AnalyticsEvent.AUDIO_SENT.NAME).push()
                        addUploadAnyAudioMedia(Utils.getPathFromUri(path))
                    }
                }
            }, {
            })
    }


    private fun liveDataObservable() {
        conversationViewModel.chatObservableLiveData.observe(this, Observer { listChat ->
            try {
                if (unlockViewHolder != null) {
                    conversationBinding.chatRv.removeView(unlockViewHolder)
                }
                chatModelLast = listChat.find { it.isSeen.not() }
                conversationList.addAll(listChat)
                val temp = listChat.groupBy { it.created }
                val tempList = temp.toSortedMap(compareBy { it })
                tempList.forEach { (key, value) ->
                    if (Utils.isSameDate(
                            lastDay,
                            key
                        ).not() || conversationBinding.chatRv.viewResolverCount == 0
                    ) {
                        conversationBinding.chatRv.addView(TimeViewHolder(key))
                        lastDay = key
                        lastMessage = null
                    }
                    value.forEach { chatModel ->
                        if (chatModelLast != null && chatModelLast == chatModel && isNewChatViewAdd && chatModelLast!!.type != BASE_MESSAGE_TYPE.UNLOCK) {
                            isNewChatViewAdd = false
                            conversationBinding.chatRv.addView(NewMessageViewHolder("Aapki Nayi Classes"))
                            linearLayoutManager.scrollToPosition(conversationBinding.chatRv.viewResolverCount - 1)
                        }
                        getView(chatModel)?.let { cell ->
                            conversationBinding.chatRv.addView(cell)
                            lastMessage = chatModel
                        }
                    }
                }
                if (isNewChatViewAdd) {
                    scrollToEnd()
                }
                readMessageDatabaseUpdate()
            } catch (ex: Exception) {
            }
        })

        conversationViewModel.refreshViewLiveData.observe(this, { chatModel ->
            val view: BaseChatViewHolder =
                conversationBinding.chatRv.getViewResolverAtPosition(conversationBinding.chatRv.viewResolverCount - 1) as BaseChatViewHolder
            view.message = chatModel
            AppObjectController.uiHandler.postDelayed({
                conversationBinding.chatRv.refreshView(view)
            }, 250)
        })
        conversationViewModel.emptyChatLiveData.observe(this, {
            hideProgressBar()
            notificationActionProcess()
        })

        conversationViewModel.userLoginLiveData.observe(this, {
            showGroupChatScreen(it)
        })

        conversationViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showProgressBar()
            } else {
                hideProgressBar()
            }
        }

    }

    private fun showGroupChatScreen(groupDetails: GroupDetails) {
        Intent(
            this,
            CometChatMessageListActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(StringContract.IntentStrings.GUID, groupDetails.groupId)
            putExtra(StringContract.IntentStrings.AVATAR, groupDetails.groupIconUrl)
            putExtra(StringContract.IntentStrings.GROUP_OWNER, groupDetails.groupOwnerUid)
            putExtra(StringContract.IntentStrings.NAME, groupDetails.groupName)
            putExtra(StringContract.IntentStrings.TYPE, CometChatConstants.RECEIVER_TYPE_GROUP)
            putExtra(StringContract.IntentStrings.MEMBER_COUNT, groupDetails.groupMemberCount)
            putExtra(StringContract.IntentStrings.GROUP_DESC, groupDetails.groupDescription)
            putExtra(StringContract.IntentStrings.GROUP_PASSWORD, groupDetails.groupPassword)
            putExtra(
                StringContract.IntentStrings.GROUP_TYPE,
                groupDetails.groupType
            )
        }.run {
            startActivity(this)
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(PlayVideoEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    lastVideoStartingDate = Date(System.currentTimeMillis())
                    conversationViewModel.setMRefreshControl(false)
                    VideoPlayerActivity.startConversionActivity(
                        this,
                        it.chatModel,
                        inboxEntity.course_name,
                        inboxEntity.duration
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(ImageShowEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Utils.fileUrl(it.localPath, it.serverPath)?.run {
                            ImageShowFragment.newInstance(this, inboxEntity.course_name, it.imageId)
                                .show(supportFragmentManager, "ImageShow")
                        }
                    },
                    {
                        it.printStackTrace()
                    })
        )
        compositeDisposable.add(
            RxBus2.listen(PdfOpenEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    PdfViewerActivity.startPdfActivity(
                        activityRef.get()!!,
                        it.pdfObject.id,
                        inboxEntity.course_name,
                        it.chatId
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(MediaProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.state == com.google.android.exoplayer2.offline.Download.STATE_COMPLETED) {
                        refreshViewAtPos(
                            AppObjectController.gsonMapperForLocal.fromJson(
                                it.id,
                                ChatModel::class.java
                            )
                        )
                    }

                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(RxBus2.listen(DownloadMediaEventBus::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                PermissionUtils.storageReadAndWritePermission(this,
                    object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            report?.areAllPermissionsGranted()?.let { flag ->
                                if (flag && internetAvailableFlag.not()) {
                                    showToast(getString(R.string.internet_not_available_msz))
                                    return@let
                                }
                                val pos =
                                    conversationBinding.chatRv.getViewResolverPosition(it.viewHolder)
                                val view: BaseChatViewHolder =
                                    conversationBinding.chatRv.getViewResolverAtPosition(pos) as BaseChatViewHolder
                                val chatModel = it.chatModel
                                chatModel.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                                view.message = it.chatModel
                                conversationBinding.chatRv.refreshView(view)
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
                    })
            })

        compositeDisposable.add(RxBus2.listen(DownloadCompletedEventBus::class.java)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val obj = AppObjectController.appDatabase.chatDao()
                        .getUpdatedChatObject(it.chatModel)
                    refreshViewAtPos(obj)
                }

            })
        compositeDisposable.add(RxBus2.listen(VideoDownloadedBus::class.java)
            .subscribeOn(Schedulers.computation())
            .subscribe {
                CoroutineScope(Dispatchers.IO).launch {
                    val chatObj = AppObjectController.appDatabase.chatDao()
                        .getUpdatedChatObjectViaId(it.messageObject.chatId)
                    refreshViewAtPos(chatObj)
                }
            })

        compositeDisposable.add(RxBus2.listen(DeleteMessageEventBus::class.java)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (removingConversationList.contains(it.chatModel)) {
                    removingConversationList.remove(it.chatModel)
                } else {
                    if (it.chatModel.isSelected) {
                        removingConversationList.add(it.chatModel)
                    }
                }
            })

        compositeDisposable.add(
            RxBus2.listen(ChatModel::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe {
                    visibleItem()
                })

        compositeDisposable.add(
            RxBus2.listen(MessageCompleteEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (conversationBinding.refreshLayout.isRefreshing) {
                        val message: String = if (it.flag) {
                            getString(R.string.new_message_arrive)
                        } else {
                            getString(R.string.no_new_message_arrive)
                        }
                        StyleableToast.Builder(this).gravity(Gravity.BOTTOM)
                            .text(message).cornerRadius(16).length(Toast.LENGTH_LONG)
                            .solidBackground().show()
                    }
                    if (it.flag.not()) {
                        hideProgressBar()
                    }
                    conversationBinding.refreshLayout.isRefreshing = false
                })

        compositeDisposable.add(
            RxBus2.listen(AudioPlayEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
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
                    setCurrentItemPosition(it.chatModel.chatId)
                    AppObjectController.currentPlayingAudioObject?.let { chatModel ->
                        refreshViewAtPos(chatModel)
                    }
                    analyticsAudioPlayed(it.audioType)

                    if (AppObjectController.currentPlayingAudioObject != null && ExoAudioPlayer.LAST_ID == it?.chatModel?.chatId) {
                        audioPlayerManager?.resumeOrPause()
                    } else {
                        audioPlayerManager?.onPause()
                        setPlayProgress(it.chatModel.playProgress)
                        AppObjectController.currentPlayingAudioObject = it.chatModel
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
                    })
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
                .subscribe({
                    AppAnalytics.create(AnalyticsEvent.PRACTICE_OPENED.NAME)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
                        .addParam(
                            AnalyticsEvent.PRACTICE_SOLVED.NAME,
                            (it.chatModel.question != null) && (it.chatModel.question!!.practiceEngagement.isNullOrEmpty()
                                .not())
                        )
                        .addParam("chatId", it.chatModel.chatId)
                        .push()
                    PractiseSubmitActivity.startPractiseSubmissionActivity(
                        activityRef.get()!!,
                        PRACTISE_SUBMIT_REQUEST_CODE,
                        it.chatModel
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(GotoChatEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    scrollToPosition(it.chatId)
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(AssessmentStartEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logAssessmentEvent(it.assessmentId)
                    AssessmentActivity.startAssessmentActivity(
                        this,
                        requestCode = ASSESSMENT_REQUEST_CODE,
                        assessmentId = it.assessmentId
                    )
                }, {
                    it.printStackTrace()
                })
        )


        compositeDisposable.add(
            RxBus2.listen(UnlockNextClassEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logUnlockCardEvent()
                    unlockViewHolder = null
                    conversationBinding.chatRv.removeView(it.viewHolder)
                    conversationViewModel.updateBatchChangeRequest()
                    conversationBinding.refreshLayout.isRefreshing = true
                    scrollToEnd()
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(ConversationPractiseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ConversationPracticeActivity.startConversationPracticeActivity(
                        this,
                        CONVERSATION_PRACTISE_REQUEST_CODE,
                        it.id,
                        it.pImage
                    )
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(P2PStartEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    /*   if (PrefManager.hasKey(IS_PRACTISE_PARTNER_VIEWED)) {
                        SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                            this,
                            inboxEntity.courseId
                        )
                    } else {
                        PractisePartnerDialogFragment.newInstance()
                            .show(supportFragmentManager, "PractisePartnerDialogFragment")
                    }
                    */
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(StartCertificationExamEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    startActivity(
                        CertificationBaseActivity.certificationExamIntent(
                            this,
                            it.certificationExamId
                        )
                    )
                }, {
                    it.printStackTrace()
                })
        )


    }

    private fun setCurrentItemPosition(chatId: String) {
        val currentChatid = chatId.toLowerCase(Locale.getDefault())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: BaseChatViewHolder
                conversationBinding.chatRv.allViewResolvers?.let {
                    it.forEachIndexed { index, view ->
                        if (view is BaseChatViewHolder) {
                            tempView = view
                            if (currentChatid == tempView.message.chatId.toLowerCase(
                                    Locale.getDefault()
                                )
                            ) {
                                currentAudioPosition = index
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                ex.printStackTrace()
            }
        }
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

    private fun refreshViewAtPos(chatObj: ChatModel, callback: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: BaseChatViewHolder
                conversationBinding.chatRv.allViewResolvers?.let {
                    it.forEachIndexed { index, view ->
                        if (view is BaseChatViewHolder) {
                            tempView = view
                            if (chatObj.chatId.toLowerCase(Locale.getDefault()) == tempView.message.chatId.toLowerCase(
                                    Locale.getDefault()
                                )
                            ) {
                                tempView.message = chatObj
                                tempView.message.isSelected = false
                                AppObjectController.uiHandler.postDelayed({
                                    conversationBinding.chatRv.refreshView(index)
                                    callback.invoke()
                                }, 250)
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                ex.printStackTrace()
            }
        }
    }


    private fun addUploadAnyAudioMedia(audioFilePath: String) {
        val recordUpdatedPath = AppDirectory.getAudioSentFile(audioFilePath).absolutePath
        AppDirectory.copy(audioFilePath, recordUpdatedPath)
        val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
        val cell =
            MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.AU,
                tAudioMessage,
                getLastMessageObject()
            )
        conversationBinding.chatRv.addView(cell)
        scrollToEnd()
        conversationViewModel.uploadMedia(
            recordUpdatedPath,
            tAudioMessage,
            cell.message
        )
    }

    private fun addUploadAudioMedia(mediaPath: String) {
        try {
            val recordUpdatedPath = AppDirectory.getAudioSentFile(null).absolutePath
            AppDirectory.copy(mediaPath, recordUpdatedPath)

            val tAudioMessage = TAudioMessage(recordUpdatedPath, recordUpdatedPath)
            val cell =
                MessageBuilderFactory.getMessage(
                    activityRef,
                    BASE_MESSAGE_TYPE.AU,
                    tAudioMessage,
                    getLastMessageObject()
                )
            conversationBinding.chatRv.addView(cell)
            scrollToEnd()
            conversationViewModel.uploadMedia(
                recordUpdatedPath,
                tAudioMessage,
                cell.message
            )
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
        }
    }


    private fun getView(
        chatModel: ChatModel
    ): BaseCell? {
        return when (chatModel.type) {
            BASE_MESSAGE_TYPE.LESSON -> {
                getGenericView(chatModel.type, chatModel)
            }
            BASE_MESSAGE_TYPE.Q -> {
                return when (chatModel.question?.type) {
                    BASE_MESSAGE_TYPE.P2P,
                    BASE_MESSAGE_TYPE.PR,
                    BASE_MESSAGE_TYPE.OTHER,
                    BASE_MESSAGE_TYPE.QUIZ,
                    BASE_MESSAGE_TYPE.TEST,
                    BASE_MESSAGE_TYPE.CE,
                    BASE_MESSAGE_TYPE.CP -> {
                        getGenericView(chatModel.question?.type, chatModel)
                    }
                    else -> {
                        getGenericView(chatModel.question?.material_type, chatModel)
                    }
                }
            }
            else -> {
                getGenericView(chatModel.type, chatModel)
            }
        }
    }

    private fun getGenericView(
        mszType: BASE_MESSAGE_TYPE?,
        chatModel: ChatModel
    ): BaseCell? {
        return when (mszType) {
            BASE_MESSAGE_TYPE.TX ->
                TextViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.IM ->
                ImageViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.AU ->
                AudioPlayerViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.PD ->
                PdfViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.VI ->
                VideoViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.PR ->
                PracticeViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.QUIZ, BASE_MESSAGE_TYPE.TEST ->
                AssessmentViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.CP ->
                ConversationPractiseViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.UNLOCK -> {
                unlockViewHolder = UnlockNextClassViewHolder(activityRef, chatModel, lastMessage)
                unlockViewHolder
                unlockViewHolder
            }
            BASE_MESSAGE_TYPE.LESSON -> {
                LessonViewHolder(activityRef, chatModel, lastMessage, this::onLessonItemClick)
            }

            BASE_MESSAGE_TYPE.P2P -> P2PViewHolder(activityRef, chatModel, lastMessage)
            BASE_MESSAGE_TYPE.CE -> CertificationExamViewHolder(activityRef, chatModel, lastMessage)

            else -> return null
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        try {
            if (requestCode == IMAGE_SELECT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                data?.let { intent ->
                    when {
                        intent.hasExtra(JoshCameraActivity.IMAGE_RESULTS) -> {
                            val returnValue =
                                intent.getStringArrayListExtra(JoshCameraActivity.IMAGE_RESULTS)
                            returnValue?.get(0)?.let { addUserImageInView(it) }
                        }
                        intent.hasExtra(JoshCameraActivity.VIDEO_RESULTS) -> {
                            val videoPath =
                                intent.getStringExtra(JoshCameraActivity.VIDEO_RESULTS)
                            videoPath?.run {
                                addUserVideoInView(this)
                            }

                        }
                        else -> return
                    }
                }
            } else if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val obj = data?.getParcelableExtra(PRACTISE_OBJECT) as ChatModel
                        val chatObj = AppObjectController.appDatabase.chatDao()
                            .getUpdatedChatObjectViaId(obj.chatId)
                        refreshViewAtPos(chatObj)
                    } catch (ex: Exception) {
                    }
                }
                showToast(getString(R.string.answer_submitted))
            } else if (requestCode == COURSE_PROGRESS_REQUEST_CODE && data != null) {
                if (data.hasExtra(PRACTISE_UPDATE_MESSAGE_KEY)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val obj: Array<String>? =
                                data.getStringArrayExtra(PRACTISE_UPDATE_MESSAGE_KEY)
                            obj?.forEach {
                                val chatObj = AppObjectController.appDatabase.chatDao()
                                    .getUpdatedChatObjectViaId(it)
                                refreshViewAtPos(chatObj)
                            }
                        } catch (ex: Exception) {
                        }
                    }
                } else if (data.hasExtra(FOCUS_ON_CHAT_ID)) {
                    scrollToPosition(data.getStringExtra(FOCUS_ON_CHAT_ID)!!)
                }
            } else if (requestCode == VIDEO_OPEN_REQUEST_CODE && data != null && data.hasExtra(
                    IS_BATCH_CHANGED
                )
            ) {
                if (data.getBooleanExtra(
                        IS_BATCH_CHANGED,
                        false
                    )
                ) {
                    AppObjectController.uiHandler.post {
                        conversationViewModel.deleteChatModelOfType(BASE_MESSAGE_TYPE.UNLOCK)
                        if (unlockViewHolder != null) {
                            conversationBinding.chatRv.removeView(unlockViewHolder)
                        }
                        fetchNewUnlockClasses(data)
                    }
                } else {
                    addUnlockNextClassCard(data)
                }
                uiHandler.postDelayed({
                    try {
                        conversationViewModel.setMRefreshControl(true)
                    } catch (ex: Exception) {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                        ex.printStackTrace()
                    }
                }, 5000)
            } else if (requestCode == ASSESSMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                CoroutineScope(Dispatchers.IO).launch {
                    data?.getStringExtra(CHAT_ROOM_ID)?.run {
                        val chatObj = AppObjectController.appDatabase.chatDao()
                            .getUpdatedChatObjectViaId(this)
                        refreshViewAtPos(chatObj)
                    }
                }
            } else if (requestCode == LESSON_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.hasExtra(
                    IS_BATCH_CHANGED
                )
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    fetchMessage()
                }
            }

        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            ex.printStackTrace()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun onLessonItemClick(lessonId: Int) {
        startActivityForResult(
            DayWiseCourseActivity.getDayWiseCourseActivityIntent(
                this,
                lessonId,
                courseId = inboxEntity.courseId
            ), LESSON_REQUEST_CODE
        )
    }

    private fun fetchNewUnlockClasses(data: Intent) {
        lastVideoStartingDate?.let { date ->
            showProgressBar()
            val tUnlockClassMessage =
                TUnlockClassMessage(getString(R.string.unlock_class_demo))
            val cell = MessageBuilderFactory.getMessage(
                activityRef,
                BASE_MESSAGE_TYPE.UNLOCK,
                tUnlockClassMessage,
                getLastMessageObject()
            )
            if (unlockViewHolder != null) {
                conversationBinding.chatRv.removeView(unlockViewHolder)
            }
            val interval = data.getIntExtra(LAST_VIDEO_INTERVAL, -1)
            val isNextVideoAvailable = data.getBooleanExtra(NEXT_VIDEO_AVAILABLE, false)
            CoroutineScope(Dispatchers.IO).launch {
                val maxInterval =
                    AppObjectController.appDatabase.chatDao()
                        .getMaxIntervalForVideo(inboxEntity.conversation_id)
                if (maxInterval == interval && isNextVideoAvailable.not() && interval < inboxEntity.duration!!) {
                    conversationViewModel.insertUnlockClassToDatabase(cell.message)
                }
                conversationViewModel.getAllUnlockedMessage(date)
            }
        }
    }

    private fun addUnlockNextClassCard(data: Intent) {
        val interval = data.getIntExtra(LAST_VIDEO_INTERVAL, -1)
        val isNextVideoAvailable = data.getBooleanExtra(NEXT_VIDEO_AVAILABLE, false)
        CoroutineScope(Dispatchers.IO).launch {
            val maxInterval =
                AppObjectController.appDatabase.chatDao()
                    .getMaxIntervalForVideo(inboxEntity.conversation_id)
            if (checkInDbForLastVideo(maxInterval, interval, isNextVideoAvailable)) {
                val tUnlockClassMessage =
                    TUnlockClassMessage(getString(R.string.unlock_class_demo))
                val cell = MessageBuilderFactory.getMessage(
                    activityRef,
                    BASE_MESSAGE_TYPE.UNLOCK,
                    tUnlockClassMessage,
                    getLastMessageObject()

                )
                CoroutineScope(Dispatchers.Main).launch {
                    if (unlockViewHolder != null) {
                        conversationBinding.chatRv.removeView(unlockViewHolder)
                    }
                }
                conversationViewModel.insertUnlockClassToDatabase(cell.message)
                AppObjectController.uiHandler.post {
                    conversationBinding.chatRv.addView(cell)
                    unlockViewHolder = cell as UnlockNextClassViewHolder
                    refreshViewAtPos(cell.message)
                }
            }
        }
    }

    private suspend fun checkInDbForLastVideo(
        maxInterval: Int,
        interval1: Int,
        nextVideoAvailable: Boolean
    ): Boolean {
        if (nextVideoAvailable)
            return false
        var interval = interval1
        if (interval == inboxEntity.duration!!) {
            return false
        }
        if (maxInterval == inboxEntity.duration!!) {
            return false
        }
        while (maxInterval > interval) {
            interval++
            val question: Question? = AppObjectController.appDatabase.chatDao()
                .getQuestionForNextInterval(
                    inboxEntity.courseId, interval
                )

            if (question != null && question.material_type == BASE_MESSAGE_TYPE.VI && question.type == BASE_MESSAGE_TYPE.Q) {
                val videoList = AppObjectController.appDatabase.chatDao()
                    .getVideosOfQuestion(questionId = question.questionId)
                if (videoList.isNullOrEmpty().not()) {
                    return false
                }
                break
            }
        }
        return true
    }

    fun uploadImageByCameraOrGallery() {
        AppAnalytics.create(AnalyticsEvent.CAMERA_CLICKED.NAME).push()
        uploadImageByUser()
    }


    private fun uploadImageByUser() {
        PermissionUtils.cameraRecordStorageReadAndWritePermission(this,
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
            })

    }

    private fun addUserImageInView(imagePath: String) {
        val imageUpdatedPath = AppDirectory.getImageSentFilePath()
        AppDirectory.copy(imagePath, imageUpdatedPath)
        val tImageMessage = TImageMessage(imageUpdatedPath, imageUpdatedPath)
        val cell = MessageBuilderFactory.getMessage(
            activityRef,
            BASE_MESSAGE_TYPE.IM,
            tImageMessage,
            getLastMessageObject()
        )
        conversationBinding.chatRv.addView(
            cell
        )
        scrollToEnd()
        conversationViewModel.uploadMedia(
            imageUpdatedPath, tImageMessage, cell.message
        )
    }

    private fun addUserVideoInView(videoPath: String) {
        val videoSentFile = AppDirectory.videoSentFile()
        AppDirectory.copy(videoPath, videoSentFile.absolutePath)
        val tVideoMessage =
            TVideoMessage(videoSentFile.absolutePath, videoSentFile.absolutePath)

        val cell = MessageBuilderFactory.getMessage(
            activityRef,
            BASE_MESSAGE_TYPE.VI,
            tVideoMessage,
            getLastMessageObject()
        )
        conversationBinding.chatRv.addView(
            cell
        )
        scrollToEnd()
        conversationViewModel.uploadMedia(
            videoSentFile.absolutePath, tVideoMessage, cell.message
        )
    }


    private fun scrollToEnd() {
        AppObjectController.uiHandler.postDelayed({
            val count = conversationBinding.chatRv.adapter?.itemCount ?: 0
            linearLayoutManager.scrollToPosition(count - 1)
            conversationBinding.scrollToEndButton.visibility = GONE
        }, 150)

    }

    private fun scrollToPosition(chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempView: BaseChatViewHolder
                conversationBinding.chatRv.allViewResolvers?.let {
                    it.forEachIndexed { index, view ->
                        if (view is BaseChatViewHolder) {
                            tempView = view
                            if (chatId.toLowerCase(Locale.getDefault()) == tempView.message.chatId.toLowerCase(
                                    Locale.getDefault()
                                )
                            ) {
                                AppObjectController.uiHandler.post {
                                    BaseChatViewHolder.sId = chatId
                                    linearLayoutManager.scrollToPositionWithOffset(index, 40)

                                }

                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                FirebaseCrashlytics.getInstance().recordException(ex)
                ex.printStackTrace()
            }
        }
    }

    fun uploadAttachment() {
        AppAnalytics.create(AnalyticsEvent.ATTACHMENT_CLICKED.NAME).push()
        AttachmentUtil.revealAttachments(revealAttachmentView, conversationBinding)
        this.revealAttachmentView = !revealAttachmentView
    }

    override fun onResume() {
        super.onResume()
        val exploreTypeStr = PrefManager.getStringValue(EXPLORE_TYPE, false)
        if (exploreTypeStr.isNotBlank()) {
            when (ExploreCardType.valueOf(exploreTypeStr)) {
                ExploreCardType.FREETRIAL -> {
                    val remainingDays = PrefManager.getIntValue(REMAINING_TRIAL_DAYS, false)
                    val isSubscriptionEnded =
                        PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED, false)
                    val isSubscriptionStarted =
                        PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED, false)
                    if (remainingDays < 0 && isSubscriptionStarted.not()) {
                        showTrialEndFragment()
                    } else if (isSubscriptionStarted && isSubscriptionEnded) {
                        showTrialEndFragment()
                    } else {
                        conversationBinding.chatRv.refresh()
                        subscribeRXBus()
                        observeNetwork()
                    }
                }
                else -> {
                    conversationBinding.chatRv.refresh()
                    subscribeRXBus()
                    observeNetwork()
                }
            }
        } else {
            conversationBinding.chatRv.refresh()
            subscribeRXBus()
            observeNetwork()
        }
    }

    override fun onPause() {
        super.onPause()
        audioPlayerManager?.onPause()
        compositeDisposable.clear()
        BaseChatViewHolder.sId = EMPTY
    }


    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        readMessageTimerTask?.cancel()
        uiHandler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppObjectController.currentPlayingAudioObject = null
        audioPlayerManager?.release()
        NPSEventModel.removeCurrentNPA()
    }

    override fun onBackPressed() {
        audioPlayerManager?.onPause()
        this@ConversationActivity.finishAndRemoveTask()
    }

    private fun openCourseProgressListingScreen() {
        AppAnalytics.create(AnalyticsEvent.COURSE_PROGRESS_OVERVIEW.NAME).push()
        CourseProgressActivity.startCourseProgressActivity(
            this,
            COURSE_PROGRESS_REQUEST_CODE,
            inboxEntity
        )
    }

    private fun observeNetwork() {
        compositeDisposable.add(
            ReactiveNetwork.observeNetworkConnectivity(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { connectivity ->
                    internetAvailableFlag = connectivity.available()
                    if (internetAvailableFlag) {
                        internetAvailable()
                    } else {
                        internetNotAvailable()
                    }
                })
    }


    private fun internetNotAvailable() {
        internetAvailableStatus.show()

    }

    private fun internetAvailable() {
        internetAvailableStatus.dismiss()

    }

    fun visibleItem() {
        CoroutineScope(Dispatchers.IO).launch {
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
                        val chatModel =
                            (conversationBinding.chatRv.getViewResolverAtPosition(
                                lastPosition
                            ) as BaseChatViewHolder).message
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
        readMessageTimerTask =
            Timer("VisibleMessage", false).scheduleAtFixedRate(5000, 1500) {
                conversationViewModel.updateInDatabaseReadMessage(readChatList)
            }
    }

    fun isAudioPlaying(): Boolean {
        return audioPlayerManager?.isPlaying()!!
    }

    private fun setPlayProgress(progress: Int) {
        AppObjectController.currentPlayingAudioObject?.playProgress = progress
        if (currentAudioPosition != -1) {
            conversationBinding.chatRv.adapter?.notifyItemChanged(
                currentAudioPosition
            )
        }
    }

    private fun showTrialEndFragment() {
        supportFragmentManager.commit(true) {
            replace(
                R.id.root_view,
                TrialEndBottomSheetFragment.newInstance(),
                TrialEndBottomSheetFragment::class.java.name
            )
        }
    }

    private fun clearMediaFromInternal() {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            MaterialDialog(this@ConversationActivity).show {
                                title(R.string.delete_media_title)
                                message(R.string.delete_media_message) {
                                    lineSpacing(1.4f)
                                }
                                positiveButton(R.string.yes) { dialog ->
                                    startDeleteMessageWorker()
                                }
                                negativeButton(R.string.no) { dialog ->
                                }
                            }
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
            })
    }

    private fun startDeleteMessageWorker() {
        val observer = Observer<WorkInfo> { workInfo ->
            try {
                workInfo?.run {
                    if (state == WorkInfo.State.ENQUEUED) {
                        showProgressBar()
                    } else if (state == WorkInfo.State.SUCCEEDED) {
                        conversationBinding.chatRv.removeAllViews()
                        conversationViewModel.refreshChat()
                        uiHandler.postDelayed({
                            hideProgressBar()
                        }, 2000)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(WorkManagerAdmin.clearMediaOfConversation(inboxEntity.conversation_id))
            .observe(this, observer)
    }

    private fun notificationActionProcess() {
        val questionId = intent.getStringExtra(QUESTION_ID) ?: EMPTY
        (intent.getSerializableExtra(ShareConstants.ACTION_TYPE) as NotificationAction?)?.let {
            if (it == NotificationAction.ACTION_OPEN_QUESTION && questionId.isNotEmpty()) {
                intent.removeExtra(QUESTION_ID)
                intent.removeExtra(ShareConstants.ACTION_TYPE)
                CoroutineScope(Dispatchers.IO).launch {
                    val question: Question? =
                        AppObjectController.appDatabase.chatDao().getQuestionOnIdV2(questionId)
                    if (question != null) {
                        val chatModel: ChatModel =
                            AppObjectController.appDatabase.chatDao()
                                .getUpdatedChatObjectViaId(question.chatId)

                        when {
                            question.type == BASE_MESSAGE_TYPE.QUIZ || question.type == BASE_MESSAGE_TYPE.TEST -> {
                                AssessmentActivity.startAssessmentActivity(
                                    this@ConversationActivity,
                                    ASSESSMENT_REQUEST_CODE,
                                    question.assessmentId ?: 0
                                )
                            }
                            question.type == BASE_MESSAGE_TYPE.CP -> {

                                chatModel.question?.conversationPracticeId?.let { cpId ->
                                    ConversationPracticeActivity.startConversationPracticeActivity(
                                        this@ConversationActivity,
                                        CONVERSATION_PRACTISE_REQUEST_CODE,
                                        cpId,
                                        chatModel.question?.imageList?.getOrNull(0)?.imageUrl
                                    )
                                }
                            }
                            question.type == BASE_MESSAGE_TYPE.PR -> {
                                PractiseSubmitActivity.startPractiseSubmissionActivity(
                                    this@ConversationActivity,
                                    PRACTISE_SUBMIT_REQUEST_CODE,
                                    chatModel
                                )
                            }
                            question.material_type == BASE_MESSAGE_TYPE.VI -> {
                                VideoPlayerActivity.startConversionActivity(
                                    this@ConversationActivity,
                                    chatModel,
                                    inboxEntity.course_name,
                                    inboxEntity.duration
                                )
                            }
                            else -> {
                                return@launch
                            }
                        }
                    }
                }
            }

        }
    }

    private fun getLastMessageObject(): ChatModel? {
        return try {
            val chatModel: ChatModel? = (conversationBinding.chatRv.getViewResolverAtPosition(
                conversationBinding.chatRv.viewResolverCount - 1
            ) as BaseChatViewHolder).message
            chatModel
        } catch (ex: java.lang.Exception) {
            null
        }

    }

    override fun onProgressUpdate(progress: Long) {
        setPlayProgress(progress.toInt())
    }

    override fun onDurationUpdate(duration: Long?) {

    }

    override fun onPlayerPause() {
        if (currentAudioPosition != -1) {
            conversationBinding.chatRv.adapter?.notifyItemChanged(
                currentAudioPosition
            )
        }
    }

    override fun onPlayerResume() {
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {

    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        audioPlayerManager?.seekTo(0)
        audioPlayerManager?.onPause()
        setPlayProgress(0)
    }

    override fun onSuccessDismiss() {
        //SearchingUserActivity.startUserForPractiseOnPhoneActivity(this, inboxEntity.courseId)
    }

    override fun onDismiss() {
    }


}
